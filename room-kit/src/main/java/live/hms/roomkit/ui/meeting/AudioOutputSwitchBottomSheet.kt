package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setDrawables
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

        var btnArray = arrayOf(
            binding.muteBtn,
            binding.speakerBtn,
            binding.wiredBtn,
            binding.bluetoothBtn,
            binding.earpieceBtn,
            binding.audioOt
        )

        val borders = arrayOf(
            binding.border1, binding.border2, binding.border3, binding.border4, binding.border5
        )

        borders.forEach {
            it.setBackgroundColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.borderDefault,
                    HMSPrebuiltTheme.getDefaults().border_bright
                )
            )
        }



        binding.closeBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }

        val devicesList = meetingViewModel.hmsSDK.getAudioDevicesList()

        if (meetingViewModel.isPeerAudioEnabled().not()) {
            binding.muteBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
        } else {
            meetingViewModel.hmsSDK.getAudioOutputRouteType().let {
                when (it) {
                    AudioDevice.BLUETOOTH -> {
                        binding.bluetoothBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
                    }

                    AudioDevice.SPEAKER_PHONE -> {
                        binding.speakerBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
                    }

                    AudioDevice.EARPIECE -> {
                        binding.earpieceBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
                    }

                    AudioDevice.WIRED_HEADSET -> {
                        binding.wiredBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
                    }

                    else -> {
                        binding.muteBtn.setDrawables(end = context?.getDrawable(R.drawable.tick))
                    }
                }
            }
        }

        btnArray.forEach {
            it.setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableEnd?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableStart?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
        }


        if (devicesList.contains(AudioDevice.BLUETOOTH)) {
            binding.bluetoothBtn.visibility = View.VISIBLE
        }

        if (devicesList.contains(AudioDevice.WIRED_HEADSET)) {
            binding.wiredBtn.visibility = View.VISIBLE
        }

        if (devicesList.contains(AudioDevice.EARPIECE)) {
            binding.earpieceBtn.visibility = View.VISIBLE
        }

        if (devicesList.contains(AudioDevice.SPEAKER_PHONE)) {
            binding.speakerBtn.visibility = View.VISIBLE
        }



        if (meetingViewModel.hmsSDK.getRoom()?.localPeer?.isWebrtcPeer() != true) {
            binding.wiredBtn.visibility = View.GONE
            binding.bluetoothBtn.visibility = View.GONE
            binding.earpieceBtn.visibility = View.GONE
            binding.muteBtn.visibility = View.GONE
        } else {
            binding.muteBtn.visibility = View.VISIBLE
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

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    private fun setAudioType(audioDevice: AudioDevice) {
        meetingViewModel.setPeerAudioEnabled(true)
        meetingViewModel.hmsSDK.switchAudioOutput(audioDevice)
        onOptionItemClicked?.invoke(meetingViewModel.hmsSDK.getAudioOutputRouteType(), true)
        dismiss()
    }
}