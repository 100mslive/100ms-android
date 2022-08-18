package live.hms.app2.ui.home

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import live.hms.app2.R
import live.hms.app2.databinding.FragmentMeetingLinkBinding
import live.hms.app2.ui.meeting.LEAVE_INFORMATION_PERSON
import live.hms.app2.ui.meeting.LEAVE_INFORMATION_REASON
import live.hms.app2.ui.meeting.LEAVE_INFROMATION_WAS_END_ROOM
import live.hms.app2.ui.settings.SettingsMode
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*


class MeetingLinkFragment : Fragment() {

    companion object {
        private const val TAG = "MeetingLinkFragment"
    }

    private var binding by viewLifecycle<FragmentMeetingLinkBinding>()
    private lateinit var settings: SettingsStore

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

    override fun onResume() {
        super.onResume()
        val data = requireActivity().intent.data
        Log.v(TAG, "onResume: Trying to update $data into EditTextMeetingUrl")

        data?.let {
            if (it.toString().isNotEmpty()) {
                val url = it.toString()
                requireActivity().intent.data = null
                if (saveTokenEndpointUrlIfValid(url)
                ) {
                    binding.edtMeetingUrl.setText(it.toString())
                }
            }
        }


        val person = requireActivity().intent.getStringExtra(LEAVE_INFORMATION_PERSON)
        val reason = requireActivity().intent.getStringExtra(LEAVE_INFORMATION_REASON)
        if (person != null && reason != null) {
            requireActivity().intent.removeExtra(LEAVE_INFORMATION_PERSON)
            requireActivity().intent.removeExtra(LEAVE_INFORMATION_REASON)
            requireActivity().intent.removeExtra(LEAVE_INFROMATION_WAS_END_ROOM)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(
                    MeetingLinkFragmentDirections.actionMeetingLinkFragmentToSettingsFragment(
                        SettingsMode.HOME
                    )
                )
            }
            R.id.action_email_logs -> {
                requireContext().startActivity(
                    EmailUtils.getNonFatalLogIntent(requireContext())
                )
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingLinkBinding.inflate(inflater, container, false)
        settings = SettingsStore(requireContext())

        setHasOptionsMenu(true)
        initOnBackPress()
        initViews()
        return binding.root
    }

    private fun initOnBackPress() {
        requireActivity().apply {
            onBackPressedDispatcher.addCallback(
                this@MeetingLinkFragment.viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
                        finish()
                    }
                })
        }
    }

    private fun validate(){
        if (REGEX_MEETING_URL_CODE.matches(binding.edtMeetingUrl.text.toString()) || REGEX_PREVIEW_URL_CODE.matches(binding.edtMeetingUrl.text.toString())) {
            enableJoinButton()
        } else {
            disableJoinButton()
        }
    }

    private fun initViews() {

        binding.edtMeetingUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validate()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        binding.edtMeetingUrl.setText(settings.lastUsedMeetingUrl)
        binding.btnJoinNow.setOnClickListener {
            (requireActivity() as HomeActivity).meetingUrl = binding.edtMeetingUrl.text.toString()
            findNavController().navigate(
                MeetingLinkFragmentDirections.actionMeetingLinkFragmentToHomeFragment()
            )
        }

        binding.btnScanNow.setOnClickListener {
            val intent = Intent(requireActivity(), QrCodeActivity::class.java)
            qrScanResultLauncher.launch(intent)
        }
    }

    private fun enableJoinButton() {
        binding.btnJoinNow.isEnabled = true
        binding.btnJoinNow.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.primary_blue_round_drawable)
        binding.btnJoinNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun disableJoinButton() {
        binding.btnJoinNow.isEnabled = false
        binding.btnJoinNow.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.primary_disabled_round_drawable)
        binding.btnJoinNow.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.material_on_background_disabled
            )
        )
    }

}