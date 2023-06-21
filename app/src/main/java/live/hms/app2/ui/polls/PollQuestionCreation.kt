package live.hms.app2.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat.OrientationMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.Orientation
import live.hms.app2.databinding.LayoutPollQuestionCreationBinding
import live.hms.app2.util.viewLifecycle

/**
 * This class creates all the questions that we're  going to need.
 * We could just directly get them into the PollBuilder class without
 * having to hold onto them separately?
 * But we also need to show them in the list so there does need to be another
 * representation.
 */
class PollQuestionCreation : Fragment() {

    private val pollsViewModel: PollsViewModel by activityViewModels()
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
        binding.createdQuestionList.adapter = adapter
        binding.createdQuestionList.layoutManager = LinearLayoutManager(requireContext())
    }
}