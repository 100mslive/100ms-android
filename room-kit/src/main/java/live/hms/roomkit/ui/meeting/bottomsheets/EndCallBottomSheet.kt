package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
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


        binding.endSessionTitle.text = "End Session"
        binding.endSessionDescription.text =
            if (meetingViewModel.isHlsRunning()) "The session will end for everyone and all the activities, including the stream will stop. You can’t undo this action." else "The session will end for everyone and all the activities will stop. You can’t undo this action."
        binding.endSessionButton.text = "End Session"

        binding.endSessionButton.setOnSingleClickListener(200L) {

            if (meetingViewModel.isHlsRunning() && meetingViewModel.isAllowedToHlsStream()) {
                meetingViewModel.stopHls()
            }
            meetingViewModel.endRoom(false)
        }


    }
}