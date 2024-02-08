package live.hms.roomkit.ui.polls.leaderboard

import android.content.res.Resources
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
import live.hms.video.polls.models.network.HMSPollResponsePeerInfo
import live.hms.video.polls.network.HMSPollLeaderboardEntry
import live.hms.video.polls.network.PollLeaderboardResponse
import live.hms.video.sdk.HmsTypedActionResultListener
import live.hms.video.sdk.models.HMSPeer


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
            sheet.behavior.skipCollapsed = true
            sheet.behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        lifecycleScope.launch {
            val pollId: String =
                (arguments?.getString(POLL_TO_DISPLAY) ?: dismissAllowingStateLoss()).toString()
            poll = meetingViewModel.getPollForPollId(pollId)
            if (poll == null) {
                // Close the fragment and exit
                dismissAllowingStateLoss()
                return@launch
            }
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
                                binding.heading.text = poll?.title
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
            model.summary?.averageTime == null || model.summary?.averageTime == 0L
        val isAverageScoreEmpty =
            model.summary?.averageScore == null
        val isCorrectAnswerEmpty =
            model.summary?.respondedCorrectlyPeersCount == null
        val isTotalPeerCountEmpty =
            model.summary?.totalPeersCount == null || model.summary?.totalPeersCount == 0


        if (isAverageScoreEmpty.not() || isAverageTimeEmpty.not() || isCorrectAnswerEmpty.not() || isTotalPeerCountEmpty.not()) {
            leaderBoardListadapter.add(LeaderBoardHeader("Participation Summary"))
        }

        val localPeer = meetingViewModel.hmsSDK.getLocalPeer()!!
        if (localPeer.peerID == poll?.createdBy?.peerID) {
            showPollCreatorSummary(
                model, isTotalPeerCountEmpty,
                isCorrectAnswerEmpty, isAverageTimeEmpty,
                isAverageScoreEmpty
            )
        } else {
            showPollParticipantSummary(model, localPeer)
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
                        timetakenStr =  millisToText(entry.duration, false, "s"),
                        correctAnswerStr = "${entry.correctResponses}/${poll?.questions?.size ?: 0}",
                        position = if (index == 0) ApplyRadiusatVertex.TOP
                        else if (index == model.entries?.size?.minus(1)) ApplyRadiusatVertex.BOTTOM
                        else ApplyRadiusatVertex.NONE,
                        rankBackGroundColor = rankTOColorMap[entry.position.toString()]?:HMSPrebuiltTheme.getColours()?.secondaryDefault
                    )
                )
            }
        }

    }

    private fun showPollParticipantSummary(
        model: PollLeaderboardResponse,
        localPeer: HMSPeer
    ) {
        val peerData : HMSPollLeaderboardEntry? = model.entries?.filter{ it.peer != null }?.find { isSelfPeer(
            it.peer!!,
            localPeer
        ) }
        if (peerData != null) with(peerData) {

            leaderBoardListadapter.add(
                LeaderBoardSubGrid(
                    "YOUR RANK",
                    "$position"
                )
            )


            leaderBoardListadapter.add(
                LeaderBoardSubGrid(
                    "POINTS", score.toString()
                )
            )

            val time = millisToText(duration, true, " secs")

            // TODO add quantity string for seconds
            leaderBoardListadapter.add(
                LeaderBoardSubGrid(
                    "TIME TAKEN", time
                )
            )

            // TODO this may show incorrect info
            leaderBoardListadapter.add(
                LeaderBoardSubGrid(
                    "CORRECT ANSWERS", "${(correctResponses ?: 0)}/${(totalResponses ?: 0)}"
                )
            )

        }

    }

    private fun showPollCreatorSummary(
        model: PollLeaderboardResponse,
        isTotalPeerCountEmpty: Boolean,
        isCorrectAnswerEmpty: Boolean,
        isAverageTimeEmpty: Boolean,
        isAverageScoreEmpty: Boolean
    ) {

        if (model.summary != null) with(model.summary!!) {
            if (isTotalPeerCountEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "VOTED",
                        "${(((respondedPeersCount?.toFloat()?:1f)/(totalPeersCount?.toFloat()?:1f)) * 100.0f).toInt()}% (${(respondedPeersCount ?: 0)}/${(totalPeersCount ?: 0)})"
                    )
                )
            }

            if (isCorrectAnswerEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "CORRECT ANSWERS", "${(((respondedCorrectlyPeersCount?.toFloat()?:1f)/(totalPeersCount?.toFloat()?:1f)) * 100.0f).toInt()}% (${(respondedCorrectlyPeersCount ?: 0)}/${(totalPeersCount ?: 0)})"
                    )
                )
            }
            if (isAverageTimeEmpty.not()) {
                leaderBoardListadapter.add(
                    LeaderBoardSubGrid(
                        "AVG. TIME TAKEN", millisToText(averageTime, false, " sec")
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
    }

    private fun isSelfPeer(peer: HMSPollResponsePeerInfo, localPeer: HMSPeer) : Boolean {
//        poll mode is empty from the server so far and can't be relied on.
//        when(poll?.mode) {
//            HmsPollUserTrackingMode.USER_ID -> peer.userid == localPeer.customerUserID
//            HmsPollUserTrackingMode.PEER_ID -> peer.peerid == localPeer.peerID
//            HmsPollUserTrackingMode.USERNAME -> peer.username == localPeer.name
//            null -> false
//        }
        return peer.userid == localPeer.customerUserID || peer.peerid == localPeer.peerID
    }
}