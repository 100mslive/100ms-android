package live.hms.roomkit.ui.polls

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutAddMoreBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizItemShortAnswerBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.saveButtonDisabled
import live.hms.roomkit.ui.theme.saveButtonEnabled
import live.hms.roomkit.util.setOnSingleClickListener

private var count: Long = 0

sealed class QuestionUi(
    open var index: Long,
    open val viewType: Int,
    open var requiredToAnswer: Boolean
) {
    // Makes the creator UI show up first.
    data class QuestionCreator(var currentQuestion: ChoiceQuestions = SingleChoiceQuestion(),
                               var isPoll : Boolean = true
    ) : QuestionUi(0, 0, false)

    sealed class ChoiceQuestions(
        override var index: Long,
        override val viewType: Int,
        override var requiredToAnswer: Boolean,
        open var withTitle: String = "",
        open var options: List<String> = emptyList(),
        open var selections : List<Int> = emptyList()
    ) : QuestionUi(index, viewType, requiredToAnswer){
        open fun isValid(isPoll : Boolean) : Boolean {
            return withTitle.isNotEmpty() &&
                    options.isNotEmpty() &&
                    options.none { it.isEmpty()}
        }
    }

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: List<Int>? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false
    ) : ChoiceQuestions(index, 1, requiredToAnswer) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || correctOptionIndex != null)
        }
    }

    data class SingleChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: Int? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false
    ) : ChoiceQuestions(index, 2, requiredToAnswer) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || correctOptionIndex != null)
        }
    }

    data class LongAnswer(
        val text: String, override var index: Long, override var requiredToAnswer: Boolean
    ) : QuestionUi(index, 3, requiredToAnswer)

    data class ShortAnswer(
        val text: String, override var index: Long, override var requiredToAnswer: Boolean
    ) : QuestionUi(index, 4, requiredToAnswer)

    object AddAnotherItemView : QuestionUi(-1, 5, false)
    object LaunchButton
}

class PollQuestionViewHolder<T : ViewBinding>(
    val binding: T,
    val saveInfo: (questionUi: QuestionUi) -> Unit,
    val isPoll: Boolean,
    val reAddQuestionCreator: () -> Unit,
    val getItem: (position: Int) -> QuestionUi.QuestionCreator
) : RecyclerView.ViewHolder(binding.root) {
    private val TAG = "PollQuestionViewHolder"

    fun bind(questionUi: QuestionUi) {
        when (questionUi) {
            is QuestionUi.QuestionCreator -> bind(questionUi)
            is QuestionUi.LongAnswer -> bind(questionUi)
            is QuestionUi.MultiChoiceQuestion -> bind(questionUi)
            is QuestionUi.ShortAnswer -> bind(questionUi)
            is QuestionUi.SingleChoiceQuestion -> bind(questionUi)
            is QuestionUi.AddAnotherItemView -> bind(questionUi)
            is QuestionUi.ChoiceQuestions -> TODO()
        }
    }

    internal fun payloadUpdate(options : QuestionCreatorChangePayload.Options) {
        // So since it was called in here, the binding must be QuestionCreatorBinding.
        if(options.newOptions == null)
            return
        with(binding as LayoutPollQuestionCreationItemBinding) {
            (optionsListView.adapter as OptionsListAdapter).submitList(options.newOptions)
        }
    }

    private fun bind(addMoreBinding: QuestionUi.AddAnotherItemView) {
        with(binding as LayoutAddMoreBinding) {
            applyTheme()
        }
        binding.addMoreOptions.setOnSingleClickListener {
            reAddQuestionCreator()
        }
    }

    private fun getTitle(binding: LayoutPollQuestionCreationItemBinding): String = with(binding) {
        askAQuestionEditText.text.toString()
    }

    private fun getSelectedQuestionType(binding: LayoutPollQuestionCreationItemBinding) =
        with(binding) {
            questionTypeSpinner.selectedItemPosition
        }

    private fun bind(questionUi: QuestionUi.QuestionCreator) {
        with(binding as LayoutPollQuestionCreationItemBinding) {
            binding.applyTheme()
            notRequiredToAnswer.setOnCheckedChangeListener { _, b ->
                getItem(bindingAdapterPosition).currentQuestion.requiredToAnswer = b
            }
            binding.askAQuestionEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    getItem(bindingAdapterPosition).currentQuestion.withTitle = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {
                    validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton)
                }

            })
            val optionsAdapter = OptionsListAdapter().also {
                it.refreshSubmitButton = { validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton) }
                it.onOptionTextChanged = { optionIndex, changedText ->
                    val question =
                        getItem(bindingAdapterPosition).currentQuestion as QuestionUi.ChoiceQuestions
                    val changedList: List<String> = question.options.toMutableList().apply {
                        this[optionIndex] = changedText
                    }
                    question.options = changedList
                    validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton)
                }
                it.onSingleOptionSelected = { position ->
                    val question =
                        getItem(bindingAdapterPosition).currentQuestion as QuestionUi.SingleChoiceQuestion
                    // This can't be right.... also we don't have any variable for selected
                    //  but not answered.
                    // This is in fact the perfect thing to create a model class for
                    // Though that model will have to be stored....
                    question.selections = listOf(position)
                    validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton)
                }
                it.onMultipleOptionSelected = { position, selected ->
                    val question =
                        getItem(bindingAdapterPosition).currentQuestion as QuestionUi.MultiChoiceQuestion

                    question.selections = if(selected)
                         question.selections.plus(position).sorted()
                    else
                        question.selections.minus(position).sorted()
                    validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton)
                }
            }
            optionsAdapter.submitList(QuestionToOptions().questionToOptions(questionUi.currentQuestion, questionUi.isPoll))
            optionsListView.adapter = optionsAdapter
            optionsListView.layoutManager = LinearLayoutManager(binding.root.context)
            val divider = DividerItemDecoration(binding.root.context, RecyclerView.VERTICAL).apply {
                setDrawable(binding.root.context.getDrawable(R.drawable.polls_creation_divider)!!)
            }
            if (isPoll) optionsListView.addItemDecoration(divider)
            questionTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    // Reset options whenever a question type is selected
                    // Add two empty options

                    val isMultiChoice = isMultiOptionQuestionCreation(questionTypeSpinner)
                    val startingOptions = listOf("", "")
//                        listOf(
//                        Option("", if(isPoll) null else isMultiChoice),
//                        Option("", if(isPoll) null else isMultiChoice)
//                    )

                    // Three different option types.
                    // 1. Poll/Quiz
                    // 2. Single choice question
                    // 3. Multi choice
//                    if(option.isChecked)
//                        selectedIndices.add(index)

                    getItem(bindingAdapterPosition).apply {
                        currentQuestion = if (isMultiChoice) {
                            QuestionUi.MultiChoiceQuestion(getTitle(binding))
                        } else {
                            QuestionUi.SingleChoiceQuestion(getTitle(binding))
                        }
                    }

                    // If short/long answer hide the options else show them
                    val multiOptionVisibility = if (position > 1) View.GONE else View.VISIBLE
                    addAnOptionTextView.visibility = multiOptionVisibility
                    optionsListView.visibility = multiOptionVisibility
                    optionsHeading.visibility = multiOptionVisibility
                    optionsHeading.text = if (position == 0) {
                        binding.root.context.getString(R.string.single_choice_text)
                    } else if (position == 1) {
                        binding.root.context.getString(R.string.multi_choice_text)
                    } else {
                        ""
                    }

                    // Only the UI might need to be toggled
                    Log.d(TAG, "Toggle UI")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
            addAnOptionTextView.setOnSingleClickListener {
                val question =
                    getItem(bindingAdapterPosition).currentQuestion as QuestionUi.ChoiceQuestions
                question.options = question.options.plus("")
                // The same item could refresh just fine, only other items need specific invocationx
            }
            deleteOptionTrashButton.setOnSingleClickListener {
                val question =
                    getItem(bindingAdapterPosition).currentQuestion as QuestionUi.ChoiceQuestions
                question.options = question.options.dropLast(1)
            }

            saveButton.saveButtonDisabled()
            saveButton.setOnClickListener {
                // Every edit text has to be disabled at this point.
                // The one for the title.
                // TODO The ones for options

                // Save the info
                saveInfo(getItem(bindingAdapterPosition).currentQuestion)
            }
        }
    }

    private fun isMultiOptionQuestionCreation(questionTypeSpinner: Spinner): Boolean {
        return questionTypeSpinner.selectedItemPosition == 1
    }

    private fun validateSaveButtonEnabledState(
        questionUi: QuestionUi.QuestionCreator, saveButton: TextView
    ) {
        // Validation criteria
        // No empty answers.
        // At least one answer.
        val isPoll = isPoll
        if (questionUi.currentQuestion.isValid(isPoll)) saveButton.saveButtonEnabled()
        else saveButton.saveButtonDisabled()
    }

    private fun bind(questionUi: QuestionUi.MultiChoiceQuestion) {

        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding) {
            questionTitle.text = questionUi.withTitle
            val adapter =
                ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
//            options.layoutManager = LinearLayoutManager(binding.root.context)
            adapter.addAll(questionUi.options)
            options.adapter = adapter
        }
    }

    private fun bind(questionUi: QuestionUi.SingleChoiceQuestion) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding) {
            questionTitle.text = questionUi.withTitle
            val adapter =
                ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
//            options.layoutManager = LinearLayoutManager(binding.root.context)
            adapter.addAll(questionUi.options)
            options.adapter = adapter
        }
    }

    private fun bind(questionUi: QuestionUi.ShortAnswer) {
        with(binding as LayoutPollQuizItemShortAnswerBinding) {
            questionNumberHeading.text = "QUESTION ${questionUi.index} : Short Answer"
            questionHeading.text = questionUi.text
        }
    }

    private fun bind(questionUi: QuestionUi.LongAnswer) {
        with(binding as LayoutPollQuizItemShortAnswerBinding) {
            questionNumberHeading.text = "QUESTION ${questionUi.index} : Long Answer"
            questionHeading.text = questionUi.text
        }
    }

}