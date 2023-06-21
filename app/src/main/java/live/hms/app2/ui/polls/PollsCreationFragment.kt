package live.hms.app2.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import live.hms.app2.databinding.FragmentParticipantsBinding
import live.hms.app2.databinding.LayoutPollsCreationBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle

/**
 * The first screen that gathers initial poll creation parameters.
 * The values are gathered into a data model in the PollsViewModel which is
 * expected to be used in further screens.
 */
class PollsCreationFragment : Fragment(){
    private var binding by viewLifecycle<LayoutPollsCreationBinding>()
    private val pollsViewModel: PollsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutPollsCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            hideVoteCount.setOnCheckedChangeListener { _, isChecked -> pollsViewModel.markHideVoteCount(isChecked) }
            pollButton.setOnSingleClickListener { highlightPollOrQuiz(true)
                pollsViewModel.highlightPollOrQuiz(true)}
            quizButton.setOnSingleClickListener { highlightPollOrQuiz(false)
                pollsViewModel.highlightPollOrQuiz(false)}
            anonymous.setOnCheckedChangeListener { _, isChecked -> pollsViewModel.isAnon(isChecked) }
            timer.setOnCheckedChangeListener{_,isChecked -> pollsViewModel.setTimer(isChecked)}
            startPollButton.setOnSingleClickListener { startPoll() }
        }
    }

    private fun startPoll() {
        // Move to the next fragment but the data is only carried forward isn't it?
        //  It's not quite used yet.
        // Perhaps it really should be a common VM for all these fragments.
        findNavController().navigate(PollsCreationFragmentDirections.actionPollsCreationFragmentToPollQuestionCreation())
    }

    private fun highlightPollOrQuiz(isPoll : Boolean) {
        // Whichever button is selected, disable it.
        // Hopefully the UI for the opposite one will be grayed.
        binding.quizButton.isEnabled = isPoll
        binding.pollButton.isEnabled = !isPoll
    }
}