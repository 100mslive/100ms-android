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

class EndCallBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "EndCallBottomSheet"
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

        val canEndRoom = meetingViewModel.isAllowedToEndMeeting()

        binding.endSessionTitle.text =  if (canEndRoom) "End Session" else "End Stream"
        binding.endSessionDescription.text = if (canEndRoom) "The session will end for everyone in the room immediately." else "The stream will end for everyone after they’ve watched it."
        binding.endSessionButton.text = if (canEndRoom) "End Session" else "End Stream"

        binding.endSessionButton.setOnSingleClickListener(200L) {

            if (canEndRoom) {
                meetingViewModel.stopHls()
                meetingViewModel.endRoom(false)
                dismissAllowingStateLoss()
            } else {
                meetingViewModel.stopHls()
                dismissAllowingStateLoss()
            }


        }


    }
}