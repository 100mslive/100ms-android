package live.hms.app2.ui.meeting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.DeviceSettingsViewBinding
import live.hms.app2.databinding.StatViewBinding
import live.hms.app2.util.viewLifecycle
import live.hms.video.utils.HmsUtilities


class DeviceStatsBottomSheet : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<DeviceSettingsViewBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DeviceSettingsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeBtn.setOnClickListener { dismiss() }

        HmsUtilities.getSupportedVp8CodecsList().forEach { codec ->

            val statView = StatViewBinding.inflate(LayoutInflater.from(context))

            statView.apply {

                val codecCapability = codec.getCapabilitiesForType(codec.supportedTypes.first())

                this.codecName.text = codec.name
                this.tvHardwareAccelarated.text = (HmsUtilities.isSoftwareOnly(codec).not()).toString()

                this.tvMaxVideoResolution.text = "${codecCapability.videoCapabilities.supportedHeights.upper}x${codecCapability.videoCapabilities.supportedWidths.upper}p"
                this.tvMinVideoResolution.text = "${codecCapability.videoCapabilities.supportedHeights.lower}x${codecCapability.videoCapabilities.supportedWidths.lower}p"


                this.tvMaxFrameRate.text = "${codecCapability.videoCapabilities.supportedFrameRates.upper}fps"
                this.tvMinFrameRate.text = "${codecCapability.videoCapabilities.supportedFrameRates.lower}fps"

            }

            binding.statViewList.addView(statView.root)
        }


    }
}