package live.hms.roomkit.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import live.hms.roomkit.databinding.FragmentParticipantsBinding
import live.hms.roomkit.databinding.LayoutPollsCreationBinding
import live.hms.roomkit.ui.meeting.MeetingFragmentDirections
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.polls.previous.PreviousPollsAdaptor
import live.hms.roomkit.ui.polls.previous.PreviousPollsInfo
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.isSelectedStroke
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.viewLifecycle

/**
 * The first screen that gathers initial poll creation parameters.
 * The values are gathered into a data model in the PollsViewModel which is
 * expected to be used in further screens.
 */
class PollsCreationFragment : Fragment(){
    private var binding by viewLifecycle<LayoutPollsCreationBinding>()
    private val pollsViewModel: PollsViewModel by activityViewModels()
    private val meetingViewModel : MeetingViewModel by activityViewModels()

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
        initOnBackPress()
        with(binding) {
            applyTheme()
            backButton.setOnSingleClickListener { findNavController().popBackStack() }
            hideVoteCount.setOnCheckedChangeListener { _, isChecked -> pollsViewModel.markHideVoteCount(isChecked) }
            pollButton.setOnSingleClickListener {
                highlightPollOrQuiz(true)
                pollsViewModel.highlightPollOrQuiz(true)
            }
            pollButton.callOnClick()
            quizButton.setOnSingleClickListener {
                highlightPollOrQuiz(false)
                pollsViewModel.highlightPollOrQuiz(false)
            }
            anonymous.setOnCheckedChangeListener { _, isChecked -> pollsViewModel.isAnon(isChecked) }
            timer.setOnCheckedChangeListener{_,isChecked -> pollsViewModel.setTimer(isChecked)}
            startPollButton.setOnSingleClickListener { startPoll() }
            val previousPollsAdaptor = PreviousPollsAdaptor{previousPollsInfo ->
                findNavController().navigate(PollsCreationFragmentDirections.actionPollsCreationFragmentToPollDisplayFragment(previousPollsInfo.pollId))
            }
            previousPolls.adapter = previousPollsAdaptor
            previousPolls.layoutManager = LinearLayoutManager(context)
            lifecycleScope.launch {
                val polls = meetingViewModel.getAllPolls()?.map { PreviousPollsInfo(it.title, it.state, it.pollId) }
                previousPollsAdaptor.submitList(polls ?: emptyList())
            }

        }
    }

    private fun startPoll() {
        pollsViewModel.setTitle(binding.pollTitleEditText.text.toString())
        // Move to the next fragment but the data is only carried forward isn't it?
        //  It's not quite used yet.
        // Perhaps it really should be a common VM for all these fragments.
        findNavController().navigate(PollsCreationFragmentDirections.actionPollsCreationFragmentToPollQuestionCreation())
    }

    private fun highlightPollOrQuiz(isPoll : Boolean) {
        // Whichever button is selected, disable it.
        // Hopefully the UI for the opposite one will be grayed.
        binding.quizButton.isSelectedStroke(!isPoll)
        binding.quizIcon.isSelectedStroke(!isPoll)

        binding.pollButton.isSelectedStroke(isPoll)
        binding.pollIcon.isSelectedStroke(isPoll)
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