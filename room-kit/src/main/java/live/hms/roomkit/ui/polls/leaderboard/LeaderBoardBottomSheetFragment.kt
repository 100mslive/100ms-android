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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutQuizLeaderboardBinding
import live.hms.roomkit.ui.meeting.InsetItemDecoration
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.polls.display.POLL_TO_DISPLAY
import live.hms.roomkit.ui.polls.display.PollDisplayFragment
import live.hms.roomkit.ui.polls.leaderboard.item.ApplyRadiusatVertex
import live.hms.roomkit.ui.polls.leaderboard.item.LeaderBoardHeader
import live.hms.roomkit.ui.polls.leaderboard.item.LeaderBoardNameSection
import live.hms.roomkit.ui.polls.leaderboard.item.LeaderBoardSubGrid
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.error.HMSException
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.network.PollLeaderboardResponse
import live.hms.video.sdk.HmsTypedActionResultListener


class LeaderBoardBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<LayoutQuizLeaderboardBinding>()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    var poll: HmsPoll? = null

    val leaderBoardListadapter = GroupieAdapter()


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

        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        lifecycleScope.launch {
            val pollId: String =
                (arguments?.getString(POLL_TO_DISPLAY) ?: dismissAllowingStateLoss()).toString()
            poll = meetingViewModel.getPollForPollId(pollId)
            leaderBoardListadapter.spanCount = 12

            binding.backButton.setOnClickListener { dismissAllowingStateLoss() }
            binding.closeBtn.setOnClickListener { dismissAllowingStateLoss() }
            binding.leaderboardRecyclerView.apply {
                adapter = leaderBoardListadapter
                layoutManager = GridLayoutManager(context, leaderBoardListadapter.spanCount).apply {
                    spanSizeLookup = leaderBoardListadapter.spanSizeLookup
                }
                addItemDecoration(
                    InsetItemDecoration(
                        getColorOrDefault(
                            HMSPrebuiltTheme.getColours()?.backgroundDefault,
                            HMSPrebuiltTheme.getDefaults().background_default
                        ), resources.getDimension(R.dimen.twelve_dp).toInt(), "inset", "inset"
                    )
                )

            }

            meetingViewModel.fetchLeaderboard(pollId,
                object : HmsTypedActionResultListener<PollLeaderboardResponse> {
                    override fun onSuccess(result: PollLeaderboardResponse) {
                        contextSafe { context, activity ->
                            activity.runOnUiThread {
                                loadList(result)
                            }
                        }
                    }

                    override fun onError(error: HMSException) {
                        contextSafe { _, activity ->
                            Toast.makeText(
                                activity,
                                "Error fetching leaderboard ${error.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissAllowingStateLoss()
                        }
                    }

                })
        }
    }

    fun loadList(model: PollLeaderboardResponse) {
        leaderBoardListadapter.clear()

        val isAverageTimeEmpty =
            model.summary?.averageTime == null || model.summary?.averageTime == 0f
        val isAverageScoreEmpty =
            model.summary?.averageScore == null || model.summary?.averageScore == 0f
        val isCorrectAnswerEmpty =
            model.summary?.respondedCorrectlyPeersCount == null || model.summary?.respondedCorrectlyPeersCount == 0
        val isTotalPeerCountEmpty =
            model.summary?.totalPeersCount == null || model.summary?.totalPeersCount == 0


        if (isAverageScoreEmpty.not() || isAverageTimeEmpty.not() || isCorrectAnswerEmpty.not() || isTotalPeerCountEmpty.not()) {
            leaderBoardListadapter.add(LeaderBoardHeader("Participation Summary"))
        }

        if (model.summary != null) with(model.summary!!) {
            if (isTotalPeerCountEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "VOTED",
                        "${(respondedPeersCount ?: 0) / (totalPeersCount ?: 1) * 100}% (${(respondedPeersCount ?: 0)}/${(totalPeersCount ?: 0)})"
                    )
                )
            }

            if (isCorrectAnswerEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "CORRECT ANSWERS", "$respondedCorrectlyPeersCount"
                    )
                )
            }
            if (isAverageTimeEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "AVG. TIME TAKEN", "${averageTime?.toInt().toString()} sec"
                    )
                )
            }

            if (isAverageScoreEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "AVG. SCORE", averageScore.toString()
                    )
                )
            }
        }


        if (model.entries.isNullOrEmpty().not()) {
            leaderBoardListadapter.add(LeaderBoardHeader("Leaderboard"))

            val rankTOColorMap = mapOf(
                "1" to "#D69516", "2" to "#3E3E3E", "3" to "#583B0F"
            )
            model.entries?.forEachIndexed { index, entry ->
                leaderBoardListadapter.add(
                    LeaderBoardNameSection(
                        titleStr = entry.peer?.username.orEmpty(),
                        subtitleStr = "${entry.score}/${poll?.questions?.map { it.weight }?.toList()?.sum()?:0} points",
                        rankStr = entry.position.toString(),
                        isSelected = true,
                        timetakenStr = "${if (entry.duration == 0L) "" else entry.duration}",
                        correctAnswerStr = "${entry.correctResponses}/${poll?.questions?.size ?: 0}",
                        position = if (index == 0) ApplyRadiusatVertex.TOP
                        else if (index == model.entries?.size?.minus(1)) ApplyRadiusatVertex.BOTTOM
                        else ApplyRadiusatVertex.NONE,
                        rankBackGroundColor = rankTOColorMap.getOrDefault(entry.position.toString(), HMSPrebuiltTheme.getColours()?.secondaryDefault)
                    )
                )
            }
        }

    }
}