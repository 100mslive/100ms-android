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
import live.hms.roomkit.databinding.ExitBottomSheetBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.enums.HMSStreamingState
import live.hms.video.utils.HMSLogger

class MultipleLeaveOptionBottomSheet() : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "LeaveBottomSheet"
    }

    private var binding by viewLifecycle<ExitBottomSheetBinding>()

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
        binding = ExitBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()

        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.leaveTitle.text = "Leave"
        binding.leaveDescription.text =
            "Others will continue after you leave. You can join the session again."

        val canEndStream = meetingViewModel.isAllowedToHlsStream()
        val canEndRoom = meetingViewModel.isAllowedToEndMeeting()

        if (canEndRoom.not() && canEndStream.not())
            dismissAllowingStateLoss()

        binding.endSessionTitle.text =
            if (canEndRoom) "End Session" else "End Stream"
        binding.endSessionDescription.text =
            if (canEndRoom) "The session will end for everyone in the room immediately." else "The stream will end for everyone after they’ve watched it."


        binding.leaveLayout.setOnSingleClickListener(200L) {
            LeaveCallBottomSheet().show(parentFragmentManager, null)
            dismissAllowingStateLoss()
        }

        binding.endSessionLayout.setOnSingleClickListener(200L) {
            EndCallBottomSheet().show(parentFragmentManager, null)
            dismissAllowingStateLoss()
        }

        binding.endSessionLayout.visibility = if(meetingViewModel.streamingState.value != HMSStreamingState.STARTED ) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun updateLayout() {
        if (meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole?.permission?.endRoom == true) {
            binding.endSessionLayout.visibility = View.VISIBLE
        }
    }

}