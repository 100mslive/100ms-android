package live.hms.roomkit.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizItemShortAnswerBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding

class PollQuestionCreatorAdapter : ListAdapter<QuestionUi, PollQuestionViewHolder<ViewBinding>>(
    DIFFUTIL_CALLBACK
) {

    init {
        // Adaptor begins with the question creation ui.
        submitList(listOf(QuestionUi.QuestionCreator))
    }

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<QuestionUi>() {
            override fun areItemsTheSame(
                oldItem: QuestionUi,
                newItem: QuestionUi
            ) = oldItem.index == newItem.index

            override fun areContentsTheSame(
                oldItem: QuestionUi,
                newItem: QuestionUi
            ) = oldItem == newItem
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PollQuestionViewHolder<ViewBinding> {
        val view = when(viewType) {
            // TODO this is where we'd left off,
            //   adding the rest of the view type layouts.
            0 -> LayoutPollQuestionCreationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            1,2 -> {
                //Multichoice question (remember to set the single choice text as well)
                LayoutPollQuizOptionsItemMultiChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            }
            3,4 -> LayoutPollQuizItemShortAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        return PollQuestionViewHolder(view!!, { submitList(currentList.plus(it)) })
    }

    override fun onBindViewHolder(holder: PollQuestionViewHolder<ViewBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).viewType

}