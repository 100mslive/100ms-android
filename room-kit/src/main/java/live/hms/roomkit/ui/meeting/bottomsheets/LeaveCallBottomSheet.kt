package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.EndSessionBottomSheetBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle

class LeaveCallBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "LeaveCallBottomSheet"
    }

    private var binding: EndSessionBottomSheetBinding by viewLifecycle()

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
        binding = EndSessionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.endSessionTitle.text = "Leave Session"
        binding.endSessionDescription.text =
            "Others will continue after you leave. You can join the session again."
        binding.endSessionButton.text = "Leave Session"

        binding.endSessionButton.setOnSingleClickListener(200L) {
            val broadcastingPeers =
                meetingViewModel.tracks.value?.filter { it.peer.hmsRole.permission.hlsStreaming }

            if (broadcastingPeers.orEmpty().size <= 1 && meetingViewModel.isHlsRunning() && meetingViewModel.isAllowedToHlsStream()) {
                meetingViewModel.stopHls()
            }
            meetingViewModel.leaveMeeting()
        }


    }
}