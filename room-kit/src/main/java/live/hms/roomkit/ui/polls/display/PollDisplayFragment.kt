package live.hms.roomkit.ui.polls.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
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
            val returnedPoll = if(pollId == null) null else meetingViewModel.getPollForPollId(pollId)
            if(returnedPoll == null) {
                // Close the fragment and exit
                findNavController().popBackStack()
                return@launch
            }

            pollsDisplayAdaptor = PollsDisplayAdaptor(
                meetingViewModel.peers.find { it.isLocal }!!,
                returnedPoll,
                meetingViewModel::saveInfoText,
                meetingViewModel::saveInfoSingleChoice,
                meetingViewModel::saveInfoMultiChoice,
                meetingViewModel::saveSkipped,
                meetingViewModel::endPoll
            )

            poll = returnedPoll

            with(binding) {
                backButton.setOnSingleClickListener {
                    parentFragmentManager
                        .beginTransaction()
                        .remove(this@PollDisplayFragment)
                        .commitAllowingStateLoss()
                }
                val startedType = if(poll.category == HmsPollCategory.QUIZ) "Quiz" else "Poll"
                pollStarterUsername.text = getString(R.string.poll_started_by,poll.startedBy?.name?: "Participant", startedType)
                questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
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
    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.backButton.callOnClick()
                }
            })
    }

}