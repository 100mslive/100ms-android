package live.hms.roomkit.ui.polls.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.databinding.LayoutQuizLeaderboardBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.polls.display.POLL_TO_DISPLAY
import live.hms.roomkit.ui.polls.display.PollDisplayFragment
import live.hms.roomkit.ui.polls.leaderboard.item.LeaderBoardHeader
import live.hms.roomkit.ui.polls.leaderboard.item.LeaderBoardSubGrid
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.error.HMSException
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.network.PollLeaderboardResponse
import live.hms.video.sdk.HmsTypedActionResultListener


class LeaderBoardBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<LayoutQuizLeaderboardBinding>()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    lateinit var poll: HmsPoll

    val leaderBoardListadapter = GroupieAdapter()

    constructor()

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

            binding.leaderboardRecyclerView.apply {
                adapter = leaderBoardListadapter
                layoutManager = GridLayoutManager(context, leaderBoardListadapter.spanCount).apply {
                    spanSizeLookup = leaderBoardListadapter.spanSizeLookup
                }
            }

            meetingViewModel.fetchLeaderboard(
                pollId,
                object : HmsTypedActionResultListener<PollLeaderboardResponse> {
                    override fun onSuccess(result: PollLeaderboardResponse) {
                        loadList(result)
                    }

                    override fun onError(error: HMSException) {
                        contextSafe { _, activity ->
                            Toast.makeText(
                                activity,
                                "Error fetching leaderboard ${error.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                })
        }
    }

    fun loadList(model: PollLeaderboardResponse) {
        leaderBoardListadapter.clear()

        if (model?.summary != null) {
            leaderBoardListadapter.add(LeaderBoardHeader("Participation Summary"))
            with(model.summary) {
                if ((totalPeersCount ?: 0) >= (respondedPeersCount
                        ?: 0) && (totalPeersCount != null || totalPeersCount != 0)
                ) {
                    leaderBoardListadapter.add(
                        LeaderBoardSubGrid(
                            "VOTED",
                            "${(respondedPeersCount ?: 0) / (totalPeersCount ?: 1) * 100}% (${(respondedPeersCount ?: 0)}/${(totalPeersCount ?: 0)})"
                        )
                    )
                }

                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "CORRECT ANSWERS", "${respondedCorrectlyPeersCount}"
                    )
                )
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "AVG. TIME TAKEN", averageTime.toString()
                    )
                )
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "AVG. SCORE", averageScore.toString()
                    )
                )
            }
        }
    }
}