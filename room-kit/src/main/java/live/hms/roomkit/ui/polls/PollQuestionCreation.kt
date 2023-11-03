package live.hms.roomkit.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    val args: PollQuestionCreationArgs by navArgs()
    private val pollsViewModel: PollsViewModel by activityViewModels()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val adapter by lazy { PollQuestionCreatorAdapter(args.isPoll, ::launchPoll) }

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
        initOnBackPress()
        with(binding) {
            heading.text = "${pollsViewModel.getPollsCreationInfo().pollTitle} ${if(pollsViewModel.isPoll()) "Poll" else "Quiz"}"
            backButton.setOnSingleClickListener { findNavController().popBackStack() }
            createdQuestionList.adapter = adapter
            createdQuestionList.layoutManager = LinearLayoutManager(requireContext())
            val divider = DividerItemDecoration(requireContext(), VERTICAL).apply {
                setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.questions_divider)!!)
            }
            createdQuestionList.addItemDecoration(divider)

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

    private fun launchPoll() {
        // Launch the poll
        meetingViewModel.startPoll(
            adapter.currentList,
            pollsViewModel.getPollsCreationInfo()
        )
        // Go back
        findNavController().popBackStack()
    }
}