package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.EndSessionBottomSheetBinding
import live.hms.roomkit.databinding.ExitBottomSheetBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.utils.HMSLogger

class EndStreamBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "EndStreamBottomSheet"
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

        if (meetingViewModel.isAllowedToHlsStream().not()) {
            binding.endSessionTitle.text = "Leave Session"
            binding.endSessionDescription.text =
                "Others will continue after you leave. You can join the session again."
            binding.endSessionButton.text = "Leave Session"
            binding.endSessionButton.setOnSingleClickListener(200L) {
                meetingViewModel.leaveMeeting()
            }
        } else {
            val isStreamIng = meetingViewModel.isHlsRunning() || meetingViewModel.isRTMPRunning()

            binding.endSessionTitle.text = if (isStreamIng) "End Stream" else "End Session"
            binding.endSessionButton.text = if (isStreamIng) "End Stream" else "End Session"
            binding.endSessionDescription.text =
                if (isStreamIng) "The stream will end for everyone after they’ve watched it." else "The session will end for everyone in the room immediately."

            binding.endSessionButton.setOnSingleClickListener(200L) {
                if (meetingViewModel.isHlsRunning()) meetingViewModel.stopHls()

                meetingViewModel.endRoom(false)
            }
        }

        HMSLogger.d(TAG, "Calling end session ...")


    }
}