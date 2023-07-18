package live.hms.app2.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import live.hms.app2.BuildConfig
import live.hms.app2.R
import live.hms.app2.databinding.FragmentHomeBinding
import live.hms.roomkit.ui.settings.SettingsMode
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.EmailUtils
import live.hms.app2.util.*
import live.hms.roomkit.util.NameUtils.isValidUserName
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.HMSRoomKit
import live.hms.roomkit.ui.meeting.*
import live.hms.roomkit.util.contextSafe

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var binding by viewLifecycle<FragmentHomeBinding>()
    private lateinit var settings: SettingsStore



    override fun onResume() {
        super.onResume()
        val data = requireActivity().intent.data
        Log.v(TAG, "onResume: Trying to update $data into EditTextMeetingUrl")

        data?.let {
            if (it.toString().isNotEmpty()) {
                val url = it.toString()
                requireActivity().intent.data = null
                if (saveTokenEndpointUrlIfValid(url) && isValidUserName(binding.editTextName)) {
                    joinRoom()
                }
            }
        }

        val person = requireActivity().intent.getStringExtra(LEAVE_INFORMATION_PERSON)
        val reason = requireActivity().intent.getStringExtra(LEAVE_INFORMATION_REASON)
        val roomWasEnded =
            requireActivity().intent.getBooleanExtra(LEAVE_INFROMATION_WAS_END_ROOM, false)

        if (person != null && reason != null) {
            requireActivity().intent.removeExtra(LEAVE_INFORMATION_PERSON)
            requireActivity().intent.removeExtra(LEAVE_INFORMATION_REASON)
            requireActivity().intent.removeExtra(LEAVE_INFROMATION_WAS_END_ROOM)
            createForceLeaveDialog(person, reason, roomWasEnded)
        }
        initOnBackPress()
    }

    private fun initOnBackPress() {
        requireActivity().apply {
            onBackPressedDispatcher.addCallback(
                this@HomeFragment.viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        Log.v(HomeFragment.TAG, "initOnBackPress -> handleOnBackPressed")
                        finish()
                    }
                })
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToSettingsFragment(SettingsMode.HOME)
                )
            }
            R.id.action_email_logs -> {
                requireContext().startActivity(
                    EmailUtils.getNonFatalLogIntent(requireContext())
                )
            }
            R.id.action_stats -> {
                val deviceStatsBottomSheet = DeviceStatsBottomSheet()
                deviceStatsBottomSheet.show(
                    requireActivity().supportFragmentManager, "deviceStatsBottomSheet"
                )
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        settings = SettingsStore(requireContext())

        setHasOptionsMenu(true)

        initEditTextViews()
        initConnectButton()

        return binding.root
    }




    private fun getUsername() = binding.editTextName.text.toString()

    private fun joinRoom() {
        settings.lastUsedMeetingUrl = settings.lastUsedMeetingUrl.replace("/preview/", "/meeting/")
        val code = getRoomCodeFromURl(settings.lastUsedMeetingUrl)
        launchPrebuilt(code!!)
    }

    private fun getRoomCodeFromURl(url: String): String? {
        return when {
            REGEX_MEETING_URL_CODE.matches(url) -> {
                val groups = REGEX_MEETING_URL_CODE.findAll(url).toList()[0].groupValues
                groups[2]
            }
            REGEX_STREAMING_MEETING_URL_ROOM_CODE.matches(url) -> {
                val groups =
                    REGEX_STREAMING_MEETING_URL_ROOM_CODE.findAll(url).toList()[0].groupValues
                groups[2]

            }
            REGEX_PREVIEW_URL_CODE.matches(url) -> {
                val groups = REGEX_PREVIEW_URL_CODE.findAll(url).toList()[0].groupValues
                groups[2]
            }
            else -> null
        }
    }

    private fun launchPrebuilt(code: String) {
        contextSafe { _, activity ->

            HMSRoomKit.launchPrebuilt(
                code, activity, HMSPrebuiltOptions(userName = getUsername(), userId = "random-user-id", debugInfo = true,
                    endPoints = hashMapOf<String, String>().apply {
                        if (settings.environment.contains("prod").not()) {
                            put("token", "https://auth-nonprod.100ms.live")
                            put("init", "https://qa-init.100ms.live/init")
                        }
                    })
            )
        }
    }


    private fun saveTokenEndpointUrlIfValid(url: String): Boolean {
        if (url.isValidMeetingUrl()) {
            settings.lastUsedMeetingUrl = url
            settings.environment = url.getInitEndpointEnvironment()
            return true
        }

        return false
    }

    private fun initEditTextViews() {
        binding.editTextName.doOnTextChanged { text, start, before, count ->
            validate()
        }

        binding.edtMeetingUrl.doOnTextChanged { text, start, before, count ->
            if (text.isNullOrEmpty()) {
                binding.tvMeetingUrlInputLayout.hint =
                    requireContext().resources.getString(R.string.paste_the_link_here_str)
            }
            validate()
        }

        binding.editTextName.setText(settings.username)
        binding.edtMeetingUrl.setText(settings.lastUsedMeetingUrl)


    }

    private fun validate() {

        if (binding.editTextName.text.isNullOrEmpty().not()
            && binding.edtMeetingUrl.text.isNullOrEmpty().not()
            &&  binding.edtMeetingUrl.text.toString().isValidMeetingUrl()) {
            enableButton()
        } else {
            disableButton()
        }
    }

    private var qrScanResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                data?.let {
                    data.getStringExtra(QrCodeActivity.QR_INTENT_RESULT)?.let {
                        if (it.isNullOrEmpty().not()) {
                            binding.edtMeetingUrl.setText(it)
                            validate()
                        }
                    }
                }
            }
        }

    private fun initConnectButton() {

        binding.btnScanNow.setOnClickListener {
            val intent = Intent(requireActivity(), QrCodeActivity::class.java)
            qrScanResultLauncher.launch(intent)
        }

        binding.btnJoinNow.setOnClickListener {
            if (binding.btnJoinNow.isEnabled.not()) {
                return@setOnClickListener
            }
            try {
                val input = binding.edtMeetingUrl.text.toString()
                if (saveTokenEndpointUrlIfValid(input) && isValidUserName(binding.editTextName)) {
                    joinRoom()
                    settings.username = binding.editTextName.text.toString()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableButton() {
        binding.btnJoinNow.isEnabled = true
        binding.btnJoinNow.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.primary_blue_round_drawable)
    }

    private fun disableButton() {
        binding.btnJoinNow.isEnabled = false
        binding.btnJoinNow.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.primary_disabled_round_drawable)

    }

    private fun createForceLeaveDialog(removedBy: String, reason: String, wasRoomEnded: Boolean) {
        val message = if (wasRoomEnded) {
            "The room was ended by ${removedBy}.\nThe reason was $reason."
        } else {
            "You were removed from the room by ${removedBy}.\nThe reason was: $reason."
        }

        val title = if (wasRoomEnded) {
            "Room Ended"
        } else {
            "Removed from the room"
        }

        val builder = AlertDialog.Builder(requireContext()).setMessage(message).setTitle(title)
            .setCancelable(false)

        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().apply { show() }

    }
}