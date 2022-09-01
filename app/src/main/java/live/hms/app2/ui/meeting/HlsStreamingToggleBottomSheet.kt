package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.HlsBottomSheetDialogBinding
import live.hms.app2.ui.meeting.participants.meetingToHlsUrl
import live.hms.app2.util.viewLifecycle
import live.hms.video.sdk.models.HMSHlsRecordingConfig


class HlsStreamingToggleBottomSheet(
    private val meetingViewModel: MeetingViewModel,
    private val meetingUrl: String,
    private val hlsStateChangeListener: (Boolean) -> Unit
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<HlsBottomSheetDialogBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            dismiss()
        }

        binding.btnGoLive.setOnClickListener {
            val videoOnDemand = binding.switchRecordStream.isChecked
            meetingViewModel.startHls(
                meetingUrl.meetingToHlsUrl(),
                recordingConfig = HMSHlsRecordingConfig(true, videoOnDemand)
            )
            meetingViewModel.hlsToggleUpdateLiveData.observe(requireActivity()) {
                if (it.not()) {
                    Toast.makeText(requireContext(), "Error Occurred! Try Again", Toast.LENGTH_LONG)
                        .show()
                }
                dismiss()
                hlsStateChangeListener.invoke(it)
            }
        }
    }
}