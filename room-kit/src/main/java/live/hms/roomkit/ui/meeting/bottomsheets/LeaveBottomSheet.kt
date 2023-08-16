package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.util.Log
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

class LeaveBottomSheet() : BottomSheetDialogFragment() {

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

        binding.leaveLayout.setOnSingleClickListener(200L) {
            HMSLogger.d(TAG, "Calling Leave ...")
            // Call leave room API
            meetingViewModel.leaveMeeting()
        }

        binding.endSessionLayout.setOnSingleClickListener(200L) {
            HMSLogger.d(TAG, "Ending Session ...")
            dismiss()
            // Call end room API
            EndStreamBottomSheet().show(parentFragmentManager, null)
        }
    }

}