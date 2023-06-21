package live.hms.app2.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import live.hms.app2.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.app2.databinding.LayoutPollQuizItemShortAnswerBinding

class PollQuestionCreatorAdapter : ListAdapter<QuestionUi, PollQuestionViewHolder<ViewBinding>>(DIFFUTIL_CALLBACK) {

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
            0 -> LayoutPollQuestionCreationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            4 -> LayoutPollQuizItemShortAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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