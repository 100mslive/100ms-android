package live.hms.app2.ui.meeting

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.GET_DEVICES_OUTPUTS
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.BottomSheetAudioSwitchBinding
import live.hms.app2.util.viewLifecycle
import live.hms.video.sdk.models.AudioOutputType


class AudioOutputSwitchBottomSheet(
    private val meetingViewModel: MeetingViewModel,
    private val audioOutputChangedListener: (AudioOutputType) -> Unit
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

        meetingViewModel.hmsSDK.getAudioOutputRouteType().let {
            when (it) {
                AudioOutputType.BLUETOOTH -> binding.bluetoothBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                AudioOutputType.SPEAKER -> binding.speakerBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                AudioOutputType.EARPIECE -> binding.earpieceBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
                else -> binding.muteBtn.background =
                    ContextCompat.getDrawable(requireContext(), R.color.color_gray_highlight)
            }
        }
        if (isBluetoothHeadsetConnected()) {
            binding.bluetoothBtn.visibility = View.VISIBLE
        }
        if (isWiredHeadSetOn()) {
            binding.wiredBtn.visibility = View.VISIBLE
        }

        binding.speakerBtn.setOnClickListener {
            meetingViewModel.hmsSDK.switchAudioOutput(AudioOutputType.SPEAKER)
            audioOutputChangedListener.invoke(AudioOutputType.SPEAKER)
            dismiss()
        }

        binding.wiredBtn.setOnClickListener {
            meetingViewModel.hmsSDK.switchAudioOutput(AudioOutputType.WIRED)
            audioOutputChangedListener.invoke(AudioOutputType.WIRED)
            dismiss()
        }

        binding.bluetoothBtn.setOnClickListener {
            meetingViewModel.hmsSDK.switchAudioOutput(AudioOutputType.BLUETOOTH)
            audioOutputChangedListener.invoke(AudioOutputType.BLUETOOTH)
            dismiss()
        }

        binding.earpieceBtn.setOnClickListener {
            meetingViewModel.hmsSDK.switchAudioOutput(AudioOutputType.EARPIECE)
            audioOutputChangedListener.invoke(AudioOutputType.EARPIECE)
            dismiss()
        }

        binding.muteBtn.setOnClickListener {
            meetingViewModel.hmsSDK.switchAudioOutput(AudioOutputType.NONE)
            audioOutputChangedListener.invoke(AudioOutputType.NONE)
            dismiss()
        }
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else {
            (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled
                    && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)
        }
    }

    private fun isWiredHeadSetOn(): Boolean {
        var isWiredHeadsetFound = false
        val audioManager: AudioManager =
            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.getDevices(GET_DEVICES_OUTPUTS).forEach {
            if (it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                isWiredHeadsetFound = true
            }
        }
        return isWiredHeadsetFound
    }
}