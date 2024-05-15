package live.hms.roomkit.ui.polls

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.selects.select
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutAddMoreBinding
import live.hms.roomkit.databinding.LayoutLaunchPollButtonBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.buttonDisabled
import live.hms.roomkit.ui.theme.buttonEnabled
import live.hms.roomkit.ui.theme.saveButtonDisabled
import live.hms.roomkit.ui.theme.saveButtonEnabled

class PollQuestionViewHolder<T : ViewBinding>(
    val binding: T,
    val saveInfo: (questionUi: QuestionUi, questionInEditIndex: Long?) -> Unit,
    val isPoll: Boolean,
    val reAddQuestionCreator: () -> Unit,
    val getItem: (position: Int) -> QuestionUi.QuestionCreator,
    val launchPoll: () -> Unit,
    val refresh: (position: Int) -> Unit,
    val editQuestion: (position: Int) -> Unit,
    val deleteQuestion: (position: Int) -> Unit,
    val updateSelection : (position : Int, List<Int>, List<String>?) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private val TAG = "PollQuestionViewHolder"

    fun bind(questionUi: QuestionUi) {
        when (questionUi) {
            is QuestionUi.QuestionCreator -> bind(questionUi)
//            is QuestionUi.LongAnswer -> bind(questionUi)
//            is QuestionUi.ShortAnswer -> bind(questionUi)
            is QuestionUi.AddAnotherItemView -> bind(questionUi)
            is QuestionUi.LaunchButton -> bind(questionUi)
            is QuestionUi.ChoiceQuestions -> bind(questionUi)

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

    private fun bind(button : QuestionUi.LaunchButton) {
        with((binding as LayoutLaunchPollButtonBinding).launchPollQuiz) {
            text = if(button.isPoll)
                "Launch Poll"
            else
                "Launch Quiz"

            if (button.enabled)
                buttonEnabled()
            else
                buttonDisabled()

            setOnSingleClickListener {
                launchPoll()
            }
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
//            <-- Removed `notRequiredToAnswer` switch for platform parity -->
//            notRequiredToAnswer.setOnCheckedChangeListener { _, b ->
//                getItem(bindingAdapterPosition).currentQuestion.requiredToAnswer = b
//            }
            binding.askAQuestionEditText.setText(questionUi.currentQuestion.withTitle)
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
                    val options = question.options
                    if(optionIndex >= options.size)
                        Log.e(TAG,"Skip invalid index change")
                    else {
                        val changedList: List<String> = options.toMutableList().apply {
                            this[optionIndex] = changedText
                        }
                        question.options = changedList
                        validateSaveButtonEnabledState(
                            getItem(bindingAdapterPosition),
                            binding.saveButton
                        )
                    }
                }
                it.onSingleOptionSelected = { position ->
                    updateSelection(bindingAdapterPosition, listOf(position), null)
                }
                it.onMultipleOptionSelected = { position, selected ->
                    val question =
                        getItem(bindingAdapterPosition).currentQuestion
                    // TODO also fix the thing
                    val selections = if(selected)
                         question.selections.plus(position).sorted()
                    else
                        question.selections.minus(position).sorted()
                    updateSelection(bindingAdapterPosition, selections, null)
                }
                it.deleteOption = { position ->
                    val question =
                        getItem(bindingAdapterPosition).currentQuestion
                    // Remove the option from the selections (answers) if it was present
                    //  Also change the index of what's selected.
                    val newSelections = question.selections.minus(position).map { selectedIndex ->
                        if(selectedIndex > position) selectedIndex - 1 else selectedIndex }
                    // Remove the option
                    val newOptions = question.options.minus(question.options[position])
                    validateSaveButtonEnabledState(getItem(bindingAdapterPosition), binding.saveButton)
                    refresh(bindingAdapterPosition)
                    updateSelection(bindingAdapterPosition, newSelections, newOptions)
                }
            }
            optionsAdapter.submitList(
                QuestionToOptions().questionToOptions(questionUi.currentQuestion, questionUi.isPoll)
            )
            optionsListView.adapter = optionsAdapter
            optionsListView.layoutManager = LinearLayoutManager(binding.root.context)
            val divider = DividerItemDecoration(binding.root.context, RecyclerView.VERTICAL).apply {
                setDrawable(binding.root.context.getDrawable(R.drawable.polls_creation_divider)!!)
            }
            var skipSelection = true
            // Remove all item decorators
            while (optionsListView.itemDecorationCount > 0) {
                optionsListView.removeItemDecorationAt(0);
            }
            // Add it back if it's polls.
            if (isPoll) optionsListView.addItemDecoration(divider)
            setCurrentSelection(questionTypeSpinner, questionUi)
            skipSelection = false
            questionTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if(skipSelection)
                        return
                    // Reset options whenever a question type is selected
                    // Add two empty options

                    val isMultiChoice = isMultiOptionQuestionCreation(questionTypeSpinner)
                    val refresh = questionUi.currentQuestion is QuestionUi.MultiChoiceQuestion && !isMultiChoice ||
                            questionUi.currentQuestion is QuestionUi.SingleChoiceQuestion && isMultiChoice

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
                    if(refresh)
                        refresh(bindingAdapterPosition)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
            addAnOptionTextView.setOnSingleClickListener {
                val question =
                    getItem(bindingAdapterPosition).currentQuestion as QuestionUi.ChoiceQuestions
                question.options = question.options.plus("")
                refresh(bindingAdapterPosition)
                // The same item could refresh just fine, only other items need specific invocationx
            }

            validateSaveButtonEnabledState(questionUi, saveButton)
            saveButton.setOnClickListener {
                // Every edit text has to have its focus cleared at this point otherwise
                // it will crash with a java.lang.IllegalArgumentException: parameter must be a descendant of this view
                //                                                                                                    	at android.view.ViewGroup.offsetRectBetweenParentAndChild(ViewGroup.java:6295).
                //  The one for the title.
                askAQuestionEditText.requestFocus() // To clear it from all others like options
                askAQuestionEditText.clearFocus()   // To clear it from even this
                // Save the info
                val questionCreatorInfo = getItem(bindingAdapterPosition)
                saveInfo(questionCreatorInfo.currentQuestion, questionCreatorInfo.questionInEditIndex)
            }
        }
    }

    private fun isMultiOptionQuestionCreation(questionTypeSpinner: Spinner): Boolean {
        return questionTypeSpinner.selectedItemPosition == 1
    }

    private fun setCurrentSelection(questionTypeSpinner: Spinner, questionUi: QuestionUi.QuestionCreator) {
        val isMulti = questionUi.currentQuestion is QuestionUi.MultiChoiceQuestion
        val selection = if(isMulti) 1 else 0
        questionTypeSpinner.setSelection(selection, false)
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

    private fun bind(questionUi: QuestionUi.ChoiceQuestions) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding) {
            applyTheme()
            editQuestionButton.setOnClickListener {
                editQuestion(bindingAdapterPosition)
            }
            deleteOptionTrashButton.setOnClickListener {
                deleteQuestion(bindingAdapterPosition)
            }
            questionTitle.text = questionUi.withTitle
            questionNumbering.text = "QUESTION ${questionUi.index} of ${questionUi.totalQuestions}: ${if(questionUi is QuestionUi.SingleChoiceQuestion) "SINGLE CHOICE" else "MULTIPLE CHOICE"}"
            val adapter = GroupieAdapter()
            adapter.addAll(questionUi.options.map {
                MultiChoiceQuestionOptionItem(questionUi, it)
            })

            val divider = DividerItemDecoration(binding.root.context, RecyclerView.VERTICAL).apply {
                setDrawable(AppCompatResources.getDrawable(binding.root.context, R.drawable.polls_creation_divider)!!)
            }
            options.addItemDecoration(divider)
            options.layoutManager = LinearLayoutManager(binding.root.context)
            options.adapter = adapter
        }
    }

}