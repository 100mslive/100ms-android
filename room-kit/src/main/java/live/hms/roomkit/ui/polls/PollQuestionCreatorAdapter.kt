package live.hms.roomkit.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutAddMoreBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizItemShortAnswerBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding

internal sealed interface QuestionCreatorChangePayload {
    data class Options(val newOptions: List<Option>?) : QuestionCreatorChangePayload

}
class PollQuestionCreatorAdapter(private val isPoll : Boolean) : ListAdapter<QuestionUi, PollQuestionViewHolder<ViewBinding>>(
    DIFFUTIL_CALLBACK
) {
    // Will be called when a single question is added to the adapter
    var isReady : ((ready : Boolean) -> Unit)? = null
    init {
        // Adaptor begins with the question creation ui.
        submitList(listOf(QuestionUi.QuestionCreator(isPoll = isPoll)))
    }

    companion object {
        val questionToOptions = QuestionToOptions()
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<QuestionUi>() {
            override fun areItemsTheSame(
                oldItem: QuestionUi,
                newItem: QuestionUi
            ) = oldItem.index == newItem.index

            override fun areContentsTheSame(
                oldItem: QuestionUi,
                newItem: QuestionUi
            ) = oldItem == newItem

            override fun getChangePayload(oldItem: QuestionUi, newItem: QuestionUi): Any? =
                when{
                    // We're only dealing with QuestionCreator changes
                    oldItem.viewType == 0 && newItem.viewType == 0 &&
                            // Options Update
                            (oldItem as QuestionUi.QuestionCreator).currentQuestion.options !=
                            (newItem as QuestionUi.QuestionCreator).currentQuestion.options
                    -> {
                        QuestionCreatorChangePayload.Options(questionToOptions.questionToOptions(newItem.currentQuestion, newItem.isPoll))
                    }
                    else -> super.getChangePayload(oldItem, newItem)
                }
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
            5 -> LayoutAddMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        return PollQuestionViewHolder(view!!, {
            val list = currentList
                .filter { item -> item.viewType != 0 }
                .plus(it)
                .plus(QuestionUi.AddAnotherItemView)
            submitList(list)
            }, isPoll,
        {
            submitList(
                listOf(QuestionUi.QuestionCreator())
                    .plus(currentList)
                    .minus(QuestionUi.AddAnotherItemView)
            )
        }) {position ->
            getItem(position) as QuestionUi.QuestionCreator
        }
    }

    override fun onBindViewHolder(holder: PollQuestionViewHolder<ViewBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: PollQuestionViewHolder<ViewBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        // Specific payloads, we'll need this to refresh the options adapter.
        // refresh only the options adapter when needed.
        payloads.forEach { payload ->
            if(payload is QuestionCreatorChangePayload.Options) {
                // Maybe better to convert to options right here.
                holder.payloadUpdate(payload)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).viewType

    override fun onCurrentListChanged(
        previousList: MutableList<QuestionUi>,
        currentList: MutableList<QuestionUi>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        val questionAdded =
            currentList.filterNot { item -> item is QuestionUi.QuestionCreator }.isNotEmpty()
        isReady?.invoke(questionAdded)
    }
}