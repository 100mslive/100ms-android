package live.hms.android100ms.ui.meeting

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.util.SettingsStore
import live.hms.android100ms.util.viewLifecycle
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MeetingFragment : Fragment() {

    companion object {
        const val RC_CALL = 111
    }

    private var binding by viewLifecycle<FragmentMeetingBinding>()
    private val args: MeetingFragmentArgs by navArgs()

    private lateinit var settingsStore: SettingsStore
    private lateinit var roomDetails: RoomDetails

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingBinding.inflate(inflater, container, false)

        settingsStore = SettingsStore(requireContext())
        roomDetails = args.roomDetail

        turnScreenOn()
        init()

        return binding.root
    }

    private fun turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            requireActivity().setTurnScreenOn(true)
        } else {
            requireActivity().window
                .addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CALL)
    private fun init() {

        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
//            initPreferences()
//            initHMSClient()
//            initializeSurfaceViews()
//            initToggleMenu()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Need User permissions to proceed",
                RC_CALL,
                *perms
            )
        }
    }

    private fun initHMSClient() {

    }


}