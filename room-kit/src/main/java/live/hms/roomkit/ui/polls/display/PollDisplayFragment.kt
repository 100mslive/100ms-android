package live.hms.roomkit.ui.polls.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.polls.models.HmsPoll

/**
 * This is shown via a toast that pops up when we receive an HmsPoll event.
 *
 */
class PollDisplayFragment : Fragment() {
    private val args: PollDisplayFragmentArgs by navArgs()
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


        lifecycleScope.launch {
            val returnedPoll = meetingViewModel.getPollForPollId(args.pollId)
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
                meetingViewModel::saveInfoMultiChoice
            )


            poll = returnedPoll

            with(binding) {
                backButton.setOnSingleClickListener {

                    findNavController().popBackStack()
                }
                pollStarterUsername.text = getString(R.string.poll_started_by,poll.startedBy?.name)
                questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
                questionsRecyclerView.adapter = pollsDisplayAdaptor
                pollsDisplayAdaptor.displayPoll(poll)
                heading.text = getString(R.string.poll_title_heading, poll.title)
                // The views have to be rendered before the update Poll Votes can be called.
                //  the delay allows for this.
                delay(300)
                pollsDisplayAdaptor.updatePollVotes(poll)
            }

            lifecycleScope.launch {
                meetingViewModel.events.onEach {
                    if (it is MeetingViewModel.Event.PollVotesUpdated) {
                        // Update the polls? How?
                        pollsDisplayAdaptor.updatePollVotes(it.hmsPoll)
                    }
                }.collect()
            }

        }
    }
}