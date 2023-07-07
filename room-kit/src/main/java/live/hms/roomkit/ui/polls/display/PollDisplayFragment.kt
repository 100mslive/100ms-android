package live.hms.roomkit.ui.polls.display

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.roomkit.databinding.LayoutPollsCreationBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.activespeaker.HlsFragmentArgs
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
        binding = LayoutPollsDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pollsDisplayAdaptor = PollsDisplayAdaptor(meetingViewModel::saveInfoText,
            meetingViewModel::saveInfoSingleChoice,
            meetingViewModel::saveInfoMultiChoice)

        poll = meetingViewModel.getPollForPollId(args.pollId)

        with(binding) {
            backButton.setOnSingleClickListener { parentFragmentManager.popBackStackImmediate() }
            questionsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            questionsRecyclerView.adapter = pollsDisplayAdaptor
            pollsDisplayAdaptor.displayPoll(poll)
        }
    }
}