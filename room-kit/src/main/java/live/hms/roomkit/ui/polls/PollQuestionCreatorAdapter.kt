package live.hms.roomkit.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutAddMoreBinding
import live.hms.roomkit.databinding.LayoutLaunchPollButtonBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding

class PollQuestionCreatorAdapter(private val isPoll : Boolean,
                                 private val launchPoll : () -> Unit) : ListAdapter<QuestionUi, PollQuestionViewHolder<ViewBinding>>(
    DIFFUTIL_CALLBACK
) {
    var count = 0L
    init {
        // Adaptor begins with the question creation ui.
        submitList(listOf(
            QuestionUi.QuestionCreator(isPoll = isPoll),
            QuestionUi.LaunchButton(isPoll, false)
        ))
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
//            3,4 -> LayoutPollQuizItemShortAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            5 -> LayoutAddMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            6 -> LayoutLaunchPollButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        return PollQuestionViewHolder(view!!,
            saveInfo = {questionUi, questionInEditIndex ->
            val list = currentList
                .filter { item -> item.viewType != 0 }
                // Either restore the question in edit index or add a new one
                .plus(questionUi.apply { index = questionInEditIndex ?: ++count })
                .plus(QuestionUi.AddAnotherItemView)
                .map {
                    if((it.viewType == 1 || it.viewType == 2)) {
                        when(it) {
                            is QuestionUi.SingleChoiceQuestion -> it.copy(totalQuestions = count)
                            is QuestionUi.MultiChoiceQuestion -> it.copy(totalQuestions = count)
                            else -> it
                        }
                    }
                    else
                        it
                }
            submitList(list.sortQuestions())
            },
            isPoll,
            reAddQuestionCreator = {
                submitList(
                    listOf(QuestionUi.QuestionCreator(isPoll = isPoll))
                        .plus(currentList)
                        .minus(QuestionUi.AddAnotherItemView).sortQuestions()
                )
            },
            getItem = {position ->
                getItem(position) as QuestionUi.QuestionCreator
            },
            launchPoll,
            refresh = {
                position -> notifyItemChanged(position)
            },
            editQuestion = {
                position ->
                val questionToEdit = getItem(position)
                val newQuestionCreator = when (questionToEdit.viewType) {
                    1 -> QuestionUi.QuestionCreator(currentQuestion = questionToEdit as QuestionUi.MultiChoiceQuestion,
                        questionInEditIndex = questionToEdit.index,
                        isPoll = isPoll)
                    2 -> QuestionUi.QuestionCreator(currentQuestion = questionToEdit as QuestionUi.SingleChoiceQuestion,
                        questionInEditIndex = questionToEdit.index,
                        isPoll = isPoll)
                    else -> null
                }
                // Delete the item and add a new question creator.
                // The index number is maintained via the question creator knowing it and sending it back.
                if(newQuestionCreator != null) {
                    // Remove the AddAnotherItem because the question creator is being added
                    submitList(currentList.minus(setOf(questionToEdit, QuestionUi.AddAnotherItemView))
                        .plus(newQuestionCreator)
                        .sortQuestions())
                }
            }
        ) { position ->
            val itemToDelete = getItem(position)
            // also every item after this should have its question number decremented.
            // also the overall count is reduced.
            count--
            val newList = currentList.minus(itemToDelete).map {
                if ((it.viewType == 1 || it.viewType == 2)) {
                    when (it) {
                        is QuestionUi.SingleChoiceQuestion -> it.copy(totalQuestions = count,
                            index = if(it.index > itemToDelete.index) it.index - 1 else it.index)
                        is QuestionUi.MultiChoiceQuestion -> it.copy(totalQuestions = count,
                            index = if(it.index > itemToDelete.index) it.index - 1 else it.index
                        )
                        else -> it
                    }
                } else
                    it
            }
            submitList(newList)
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
        // Any questions that exist are defacto valid otherwise they couldn't have been added
        val shouldEnableLaunchPollButton =
            currentList.filter { item ->
                // Only the question types
                item.viewType in 1..4
            }.isNotEmpty()

        currentList.find { it.viewType == 6 }?.let { launchButton ->
            val shouldNotify = (launchButton as QuestionUi.LaunchButton).
                enabled != shouldEnableLaunchPollButton

            if(shouldNotify) {
                submitList(currentList.filterNot { it.viewType == 6 }
                    .plus(launchButton.copy(enabled = shouldEnableLaunchPollButton)).sortQuestions())
            }

        }

    }

    override fun getItemId(position: Int): Long {
        return getItem(position).getItemId()
    }
}

internal sealed interface QuestionCreatorChangePayload {
    data class Options(val newOptions: List<Option>?) : QuestionCreatorChangePayload

}

fun List<QuestionUi>.sortQuestions() : List<QuestionUi>{
    return sortedWith(compareBy(QuestionUi::getItemId))
}