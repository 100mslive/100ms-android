package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ExitBottomSheetBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
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

        binding.leaveTitle.text = "Leave"
        binding.leaveDescription.text =
            "Others will continue after you leave. You can join the session again."

        binding.endSessionTitle.text =
            if (meetingViewModel.isHlsRunning()) "End Session" else "End for All"
        binding.endSessionDescription.text =
            if (meetingViewModel.isHlsRunning()) "The session and stream will end for everyone. You can’t undo this action." else "The session will end for everyone. You can’t undo this action."


        binding.leaveLayout.setOnSingleClickListener(200L) {
            LeaveCallBottomSheet().show(parentFragmentManager, null)
            dismissAllowingStateLoss()
        }

        binding.endSessionLayout.setOnSingleClickListener(200L) {
            EndCallBottomSheet().show(parentFragmentManager, null)
            dismissAllowingStateLoss()
        }
    }

    private fun updateLayout() {
        if (meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole?.permission?.endRoom == true) {
            binding.endSessionLayout.visibility = View.VISIBLE
        }
    }

}