package live.hms.roomkit.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat.DividerMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollQuestionCreationBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.viewLifecycle

/**
 * This class creates all the questions that we're  going to need.
 * We could just directly get them into the PollBuilder class without
 * having to hold onto them separately?
 * But we also need to show them in the list so there does need to be another
 * representation.
 */
class PollQuestionCreation : Fragment() {

    private val pollsViewModel: PollsViewModel by activityViewModels()
    private val meetingViewModel : MeetingViewModel by activityViewModels()
    private val adapter = PollQuestionCreatorAdapter()

    private var binding by viewLifecycle<LayoutPollQuestionCreationBinding>()
    /**
     * Fundamentally this contains
     * 1. A list of questions
     * 2. First item in that list is the one that lets you set new questions.
     * 3. Subsequently you see all the questions already created under that. In the order that
     *  they were created.
     * 4. So this is a multi-UI recyclerview that keeps items it creates until they're ready.
     * Should this just use a poll builder as the question storage? Sure why make a new one?
     *
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutPollQuestionCreationBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnSingleClickListener { parentFragmentManager.popBackStack() }
        binding.createdQuestionList.adapter = adapter
        binding.createdQuestionList.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), VERTICAL).apply {
            setDrawable(binding.root.context.getDrawable(R.drawable.questions_divider)!!)
        }
        binding.createdQuestionList.addItemDecoration(divider)

        binding.launchPollQuiz.setOnSingleClickListener {
            // Clear the UI
            // start the data
            meetingViewModel.startPoll(adapter.currentList, pollsViewModel.getPollsCreationInfo())
            binding.backButton.callOnClick()
        }
    }
}