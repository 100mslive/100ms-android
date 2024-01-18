package live.hms.roomkit.ui.polls.display

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.polls.leaderboard.LeaderBoardBottomSheetFragment
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.pollsStatusLiveDraftEnded
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.HmsPollCategory
import live.hms.video.polls.models.HmsPollState


/**
 * This is shown via a toast that pops up when we receive an HmsPoll event.
 *
 */
const val POLL_TO_DISPLAY = "pollId"
class PollDisplayFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG: String = "PollDisplayFragment"
        fun launch(pollId : String, fm : FragmentManager) {
            val args = Bundle()
                .apply {
                    putString(POLL_TO_DISPLAY, pollId)
                }

            PollDisplayFragment()
                .apply { arguments = args }
                .show(
                    fm,
                    PollDisplayFragment.TAG
                )
        }
    }

    private var binding by viewLifecycle<LayoutPollsDisplayBinding>()
    lateinit var pollsDisplayAdaptor: PollsDisplayAdaptor
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    lateinit var poll : HmsPoll

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutPollsDisplayBinding.inflate(inflater, container, false).also { it.applyTheme() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initOnBackPress()
        lifecycleScope.launch {
            val pollId = arguments?.getString(POLL_TO_DISPLAY)
            val returnedPoll = if(pollId.isNullOrEmpty()) null else meetingViewModel.getPollForPollId(pollId)
            if(returnedPoll == null) {
                // Close the fragment and exit
                closePollDisplay()
                return@launch
            }

            pollsDisplayAdaptor = PollsDisplayAdaptor(
                meetingViewModel.peers.find { it.isLocal }!!,
                returnedPoll,
                meetingViewModel::saveInfoText,
                { q,i,p,t ->
                    answerSelected()
                    meetingViewModel.saveInfoSingleChoice(q,i,p,t)
                } ,
                { q,i,p,t ->
                    answerSelected()
                    meetingViewModel.saveInfoMultiChoice(q,i,p,t)},
                meetingViewModel::saveSkipped,
                meetingViewModel::endPoll,
                meetingViewModel.getQuestionStartTime,
                meetingViewModel.setQuestionStartTime
            ) { LeaderBoardBottomSheetFragment.launch(it, requireFragmentManager()) }

            poll = returnedPoll

            with(binding) {
                backButton.setOnSingleClickListener {
                    closePollDisplay()
                }
                val startedType = if(poll.category == HmsPollCategory.QUIZ) "Quiz" else "Poll"
                pollStarterUsername.text = getString(R.string.poll_started_by,poll.startedBy?.name?: "Participant", startedType)

                // Quizzes only scroll horizontally and snap to questions
                if(poll.category == HmsPollCategory.QUIZ && poll.state == HmsPollState.STARTED) {
                    val touchListener = QuizDisableSwipingConditionally(::isQuestionAnswered)
                    questionsRecyclerView.addOnItemTouchListener(touchListener)
                    questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
                    /**
                     * We have to start tracking the time the question is being taken to answer
                     * from the time it's displayed.
                     * This is complicated by the fact that recyclerview preloads views.
                     * So if you just try to save the time from when it's bound, the first
                     * view will be ok, even the second but the third will be preloaded.
                     * Thus messing up how long it really took.
                     * So we need two things to be able to track this correctly:
                     * 1. On a scroll, we check the current visible item and mark it as seen.
                     * 2. The very first view won't be saved this way since it was never scrolled to.
                     *      So that has to be saved separately, which we will do in the onBind.
                     */
                    questionsRecyclerView.addOnScrollListener(object :
                        RecyclerView.OnScrollListener() {

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                            val question = pollsDisplayAdaptor.getItemForPosition(position)
                            if(question is QuestionContainer.Question) {
                                meetingViewModel.setQuestionStartTime(question)
                                touchListener.isMoving = false
                            }
                        }
                    })
                    PagerSnapHelper().attachToRecyclerView(questionsRecyclerView)
                } else {
                    questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
                }
                questionsRecyclerView.adapter = pollsDisplayAdaptor
                pollsDisplayAdaptor.displayPoll(poll)
                pollsLive.pollsStatusLiveDraftEnded(poll.state)
                heading.text = getString(R.string.poll_title_heading, poll.title, startedType)
                // The views have to be rendered before the update Poll Votes can be called.
                //  the delay allows for this.
                delay(300)
                pollsDisplayAdaptor.updatePollVotes(poll)
            }

            lifecycleScope.launch {
                meetingViewModel.events.onEach {
                    if (it is MeetingViewModel.Event.PollVotesUpdated) {
                        pollsDisplayAdaptor.updatePollVotes(it.hmsPoll)
                    }
                    if( it is MeetingViewModel.Event.PollEnded) {
                        if(pollsDisplayAdaptor.getPoll.pollId == it.hmsPoll.pollId) {
                            binding.pollsLive.pollsStatusLiveDraftEnded(HmsPollState.STOPPED)
                            binding.questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
                            pollsDisplayAdaptor.notifyDataSetChanged()
                            // Doesn't show up without the delay
                            delay(300)
                            pollsDisplayAdaptor.updatePollVotes(it.hmsPoll)
                        }
                    }
                }.collect()
            }

        }
    }

    private fun closePollDisplay() {
        parentFragmentManager
            .beginTransaction()
            .remove(this@PollDisplayFragment)
            .commitAllowingStateLoss()
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.backButton.callOnClick()
                }
            })
    }
    private fun isQuestionAnswered(position: Int): Boolean {
        if(position == NO_POSITION) return false
        val item = pollsDisplayAdaptor.getItemForPosition(position)
        return item is QuestionContainer.Question && item.voted
    }

    // Scroll when the answer is selected
    private fun answerSelected() {
        val position = (binding.questionsRecyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if(position != NO_POSITION &&  position + 1 < pollsDisplayAdaptor.itemCount )
            binding.questionsRecyclerView.smoothScrollToPosition(position + 1)
    }
}