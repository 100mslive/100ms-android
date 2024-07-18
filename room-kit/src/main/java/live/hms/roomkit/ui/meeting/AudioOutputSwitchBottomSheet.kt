package live.hms.roomkit.ui.meeting

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.GroupieAdapter
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setDrawables
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager.AudioDevice


class AudioOutputSwitchBottomSheet(
    private val onOptionItemClicked: ((AudioDevice?, Boolean) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetAudioSwitchBinding>()

    private val audioDeviceAdapter = GroupieAdapter()


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
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.audioOt.drawableStart?.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.audioOt.setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )




        binding.border5.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.borderDefault,
                HMSPrebuiltTheme.getDefaults().border_bright
            )
        )


        binding.root.background =
            binding.root.context.resources.getDrawable(R.drawable.gray_shape_round_dialog).apply {
                val color = getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.backgroundDefault,
                    HMSPrebuiltTheme.getDefaults().background_default
                )
                setColorFilter(color, PorterDuff.Mode.ADD);
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

        audioDeviceAdapter.clear()
        val devicesList = meetingViewModel.hmsSDK.getAudioDevicesInfoList()

        //skips for HLS playback
        val showAudioOption = meetingViewModel.hmsSDK.getRoom()?.localPeer?.isWebrtcPeer()
        var isMute: Boolean = false
        var selectedDeviceType: AudioDevice? = null

        if (meetingViewModel.isPeerAudioEnabled().not()) {
            isMute = true
        } else {
            selectedDeviceType = meetingViewModel.hmsSDK.getAudioOutputRouteType()
        }

        if (showAudioOption == true) {
            for (deviceInfo in devicesList) {

                //backward compatibility handling
                val isSelected = (selectedDeviceType == deviceInfo.type)
                audioDeviceAdapter.add(
                    AudioItem(
                        title = capitalizeAndReplaceUnderscore(deviceInfo.type.name),
                        subTitle= deviceInfo.name.orEmpty(),
                        isSelected = isSelected,
                        type = deviceInfo.type,
                        drawableRes = getDrawableBasedOnDeviceType(deviceInfo.type),
                        onClick = { type, id ->
                            setAudioType(type)
                        })
                )


            }
        }



        audioDeviceAdapter.add(
            AudioItem(title = "Mute",
                isSelected = isMute,
                drawableRes = R.drawable.ic_volume_off_24,
                onClick = { type, id ->
                    meetingViewModel.setPeerAudioEnabled(false)
                    onOptionItemClicked?.invoke(null, true)
                    dismiss()
                })
        )





        binding.deviceList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = audioDeviceAdapter
        }
    }

    fun getDrawableBasedOnDeviceType(device: AudioDevice): Int = when (device) {
        AudioDevice.BLUETOOTH -> {
            R.drawable.bt
        }

        AudioDevice.SPEAKER_PHONE -> {
            R.drawable.ic_icon_speaker
        }

        AudioDevice.EARPIECE -> {
            R.drawable.phone
        }

        AudioDevice.WIRED_HEADSET -> {
            R.drawable.wired
        }

        AudioDevice.AUTOMATIC -> R.drawable.ic_icon_speaker
    }


    fun capitalizeAndReplaceUnderscore(input: String): String {
        if (input.isEmpty()) {
            return ""
        }

        // Capitalize the first character
        val capitalized = input.toLowerCase().replaceFirstChar { it.uppercase() }

        // Replace underscores with spaces
        return capitalized.replace('_', ' ')
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