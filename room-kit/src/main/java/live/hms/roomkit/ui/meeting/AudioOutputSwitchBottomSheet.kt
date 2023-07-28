package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.setBackgroundAndColor
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager.AudioDevice


class AudioOutputSwitchBottomSheet(
    private val onOptionItemClicked: ((AudioDevice?, Boolean) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetAudioSwitchBinding>()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAudioSwitchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDefault,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )

        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }

        val devicesList = meetingViewModel.hmsSDK.getAudioDevicesList()

        if (meetingViewModel.isPeerAudioEnabled().not()) {
//            binding.muteBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
        } else {
            meetingViewModel.hmsSDK.getAudioOutputRouteType().let {
//                when (it) {
//                    AudioDevice.BLUETOOTH ->
//                        binding.bluetoothBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
//
//                    AudioDevice.SPEAKER_PHONE ->
//                        binding.speakerBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
//                    AudioDevice.EARPIECE -> binding.earpieceBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
//                    AudioDevice.WIRED_HEADSET -> binding.wiredBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
//                    else -> binding.speakerBtn.setBackgroundAndColor(HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
//                }
            }
        }

        if (devicesList.contains(AudioDevice.BLUETOOTH)) {
            binding.bluetoothBtn.visibility = View.VISIBLE
        }

        if (devicesList.contains(AudioDevice.WIRED_HEADSET)) {
            binding.wiredBtn.visibility = View.VISIBLE
        }

        if (devicesList.contains(AudioDevice.EARPIECE)){
            binding.earpieceBtn.visibility = View.VISIBLE
        }

        if (meetingViewModel.hmsSDK.getRoom()?.localPeer?.isWebrtcPeer() != true){
            binding.wiredBtn.visibility = View.GONE
            binding.bluetoothBtn.visibility = View.GONE
            binding.earpieceBtn.visibility = View.GONE
        }

        binding.speakerBtn.setOnClickListener {
            setAudioType(AudioDevice.SPEAKER_PHONE)
        }

        binding.wiredBtn.setOnClickListener {
            setAudioType(AudioDevice.WIRED_HEADSET)
        }

        binding.bluetoothBtn.setOnClickListener {
            setAudioType(AudioDevice.BLUETOOTH)
        }

        binding.earpieceBtn.setOnClickListener {
            setAudioType(AudioDevice.EARPIECE)
        }

        binding.muteBtn.setOnClickListener {
            meetingViewModel.setPeerAudioEnabled(false)
            onOptionItemClicked?.invoke(null, true)
            dismiss()
        }
    }

    private fun setAudioType(audioDevice: AudioDevice) {
        meetingViewModel.setPeerAudioEnabled(true)
        meetingViewModel.hmsSDK.switchAudioOutput(audioDevice)
        onOptionItemClicked?.invoke(meetingViewModel.hmsSDK.getAudioOutputRouteType(), true)
        dismiss()
    }
}