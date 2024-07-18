package live.hms.roomkit.ui.diagnostic.fragments

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
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.meeting.AudioItem
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager

class PreCallAudioSwitchDialog(
    private val onOptionItemClicked: ((HMSAudioManager.AudioDevice?, Boolean) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetAudioSwitchBinding>()

    private val audioDeviceAdapter = GroupieAdapter()


    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
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
        val devicesList = vm.getAudioDevicesInfoList()

        //skips for HLS playback

        var isMute: Boolean = false
        var selectedDeviceType: HMSAudioManager.AudioDevice? = null


        selectedDeviceType = vm.getAudioOutputRouteType()


        for (deviceInfo in devicesList) {

            //backward compatibility handling
            val isSelected = (selectedDeviceType == deviceInfo.type)
            audioDeviceAdapter.add(
                AudioItem(title = capitalizeAndReplaceUnderscore(deviceInfo.type.name),
                    subTitle = deviceInfo.name.orEmpty(),
                    isSelected = isSelected,
                    type = deviceInfo.type,
                    drawableRes = getDrawableBasedOnDeviceType(deviceInfo.type),
                    onClick = { type, id ->
                        setAudioType(type)
                    })
            )
        }








        binding.deviceList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = audioDeviceAdapter
        }
    }

    fun getDrawableBasedOnDeviceType(device: HMSAudioManager.AudioDevice): Int = when (device) {
        HMSAudioManager.AudioDevice.BLUETOOTH -> {
            R.drawable.bt
        }

        HMSAudioManager.AudioDevice.SPEAKER_PHONE -> {
            R.drawable.ic_icon_speaker
        }

        HMSAudioManager.AudioDevice.EARPIECE -> {
            R.drawable.phone
        }

        HMSAudioManager.AudioDevice.WIRED_HEADSET -> {
            R.drawable.wired
        }

        HMSAudioManager.AudioDevice.AUTOMATIC -> R.drawable.ic_icon_speaker
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

    private fun setAudioType(audioDevice: HMSAudioManager.AudioDevice) {
        vm.switchAudioOutput(audioDevice)
        onOptionItemClicked?.invoke(vm.getAudioOutputRouteType(), true)
        dismiss()
    }
}