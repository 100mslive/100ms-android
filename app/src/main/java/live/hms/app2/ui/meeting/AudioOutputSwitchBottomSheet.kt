package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.BottomSheetAudioSwitchBinding
import live.hms.app2.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager.AudioDevice


class AudioOutputSwitchBottomSheet(
    private val meetingViewModel: MeetingViewModel,
    private var isPreview: Boolean = false,
    private val onOptionItemClicked: ((AudioDevice?, Boolean) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetAudioSwitchBinding>()

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

        val devicesList = meetingViewModel.hmsSDK.getAudioDevicesList()

        if (meetingViewModel.isPeerAudioEnabled().not()) {
            binding.muteBtn.background =
                ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
        } else {
            meetingViewModel.hmsSDK.getAudioOutputRouteType().let {
                when (it) {
                    AudioDevice.BLUETOOTH -> binding.bluetoothBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                    AudioDevice.SPEAKER_PHONE -> binding.speakerBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                    AudioDevice.EARPIECE -> binding.earpieceBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                    AudioDevice.WIRED_HEADSET -> binding.wiredBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                    else -> binding.speakerBtn.background =
                        ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                }
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