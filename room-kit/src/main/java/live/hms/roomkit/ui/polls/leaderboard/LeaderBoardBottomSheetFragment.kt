package live.hms.roomkit.ui.polls.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import live.hms.roomkit.databinding.LayoutPollsDisplayBinding
import live.hms.roomkit.databinding.LayoutQuizLeaderboardBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.polls.display.POLL_TO_DISPLAY
import live.hms.roomkit.ui.polls.display.PollDisplayFragment
import live.hms.roomkit.ui.polls.display.PollsDisplayAdaptor
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.error.HMSException
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.network.PollLeaderboardResponse
import live.hms.video.sdk.HmsTypedActionResultListener

class LeaderBoardBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<LayoutQuizLeaderboardBinding>()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    lateinit var poll: HmsPoll

    companion object {
        const val TAG: String = "LeaderBoardBottomSheetFragment"
        fun launch(pollId: String, fm: FragmentManager) {
            val args = Bundle().apply {
                putString(POLL_TO_DISPLAY, pollId)
            }

            LeaderBoardBottomSheetFragment().apply { arguments = args }.show(
                fm, PollDisplayFragment.TAG
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LayoutQuizLeaderboardBinding.inflate(inflater, container, false)
            .also { it.applyTheme() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val pollId: String =
                (arguments?.getString(POLL_TO_DISPLAY) ?: dismissAllowingStateLoss()).toString()
            val poll = meetingViewModel.getPollForPollId(pollId) ?: {
                dismissAllowingStateLoss()
            }

            meetingViewModel.fetchLeaderboard(pollId,
                object : HmsTypedActionResultListener<PollLeaderboardResponse> {
                    override fun onSuccess(result: PollLeaderboardResponse) {

                    }

                    override fun onError(error: HMSException) {
                    }

                })
        }
    }
}

}