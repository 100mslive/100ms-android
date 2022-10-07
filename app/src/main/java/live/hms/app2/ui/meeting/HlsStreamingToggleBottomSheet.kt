package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.HlsBottomSheetDialogBinding
import live.hms.app2.ui.meeting.participants.meetingToHlsUrl
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle
import live.hms.video.sdk.models.HMSHlsRecordingConfig


class HlsStreamingToggleBottomSheet(
    private val meetingUrl: String,
    private val hlsStateChangeListener: (Boolean) -> Unit
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<HlsBottomSheetDialogBinding>()
    private val meetingViewModel: MeetingViewModel by activityViewModels()

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

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.btnGoLive.apply {
            setOnSingleClickListener(1000) {
                val videoOnDemand = binding.switchRecordStream.isChecked
                meetingViewModel.startHls(
                    meetingUrl.meetingToHlsUrl(),
                    recordingConfig = HMSHlsRecordingConfig(true, videoOnDemand)
                )
                meetingViewModel.hlsToggleUpdateLiveData.observe(requireActivity()) {
                    dismiss()
                    hlsStateChangeListener.invoke(it)
                }
            }
        }
    }
}