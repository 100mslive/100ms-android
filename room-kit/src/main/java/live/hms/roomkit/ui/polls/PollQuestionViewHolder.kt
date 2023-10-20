package live.hms.roomkit.ui.polls

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
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

private var count : Long = 0
sealed class QuestionUi(open val index : Long, open val viewType : Int, open val requiredToAnswer : Boolean){
    // Makes the creator UI show up first.
    object QuestionCreator : QuestionUi(0, 0, false)

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(val withTitle: String, val options: List<String>, val correctOptionIndex: List<Int>?, override val index : Long,
                                   override val requiredToAnswer: Boolean) : QuestionUi(index, 1, requiredToAnswer)
    data class SingleChoiceQuestion(val withTitle: String,
                                    val options: List<String>,
                                    val correctOptionIndex: Int?,
                                    override val index : Long,
                                    override val requiredToAnswer: Boolean) : QuestionUi(index, 2, requiredToAnswer)
    data class LongAnswer(val text : String, override val index: Long,
                          override val requiredToAnswer: Boolean) : QuestionUi(index, 3, requiredToAnswer)
    data class ShortAnswer(val text : String, override val index: Long,
                           override val requiredToAnswer: Boolean) : QuestionUi(index, 4, requiredToAnswer)
    object AddAnotherItemView : QuestionUi(-1, 5, false)
    object LaunchButton
}

class PollQuestionViewHolder<T : ViewBinding>(val binding: T,
                                              val saveInfo : (questionUi: QuestionUi) -> Unit,
    val isPoll : () -> Boolean,
                                              val reAddQuestionCreator: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val TAG = "PollQuestionViewHolder"

    fun bind(questionUi: QuestionUi) {
        when(questionUi) {
            is QuestionUi.QuestionCreator -> bind(QuestionUi.QuestionCreator)
            is QuestionUi.LongAnswer -> bind(questionUi)
            is QuestionUi.MultiChoiceQuestion -> bind(questionUi)
            is QuestionUi.ShortAnswer -> bind(questionUi)
            is QuestionUi.SingleChoiceQuestion -> bind(questionUi)
            is QuestionUi.AddAnotherItemView -> bind(questionUi)
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
    private fun bind(questionUi: QuestionUi.QuestionCreator) {
        with(binding as LayoutPollQuestionCreationItemBinding) {
            binding.applyTheme()
            val requiredToAnswer = !notRequiredToAnswer.isChecked
            val optionsAdapter = OptionsListAdapter().also {
                it.refreshSubmitButton = { validateSaveButtonEnabledState(it, binding.saveButton) }
            }
            optionsListView.adapter = optionsAdapter
            optionsListView.layoutManager = LinearLayoutManager(binding.root.context)
            val divider =
                DividerItemDecoration(binding.root.context, RecyclerView.VERTICAL).apply {
                    setDrawable(binding.root.context.getDrawable(R.drawable.polls_creation_divider)!!)
                }
            if(isPoll())
                optionsListView.addItemDecoration(divider)
            questionTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    // Reset options whenever a question type is selected
                    // Add two empty options
                    val isPoll = isPoll()
                    optionsAdapter.submitList(listOf(
                        Option("", if(isPoll) null else isMultiOptionQuestionCreation(questionTypeSpinner)),
                        Option("", if(isPoll) null else isMultiOptionQuestionCreation(questionTypeSpinner))
                    ))

                    // If short/long answer hide the options else show them
                    val multiOptionVisibility = if(position > 1) View.GONE else View.VISIBLE
                    addAnOptionTextView.visibility = multiOptionVisibility
                    optionsListView.visibility = multiOptionVisibility
                    optionsHeading.visibility = multiOptionVisibility
                    optionsHeading.text = if(position == 0 ) {
                        binding.root.context.getString(R.string.single_choice_text)
                    } else if(position == 1 ){
                        binding.root.context.getString(R.string.multi_choice_text)
                    } else {
                        ""
                    }

                    // Only the UI might need to be toggled
                    Log.d(TAG,"Toggle UI")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
            addAnOptionTextView.setOnSingleClickListener {
                val showCheckBox = isMultiOptionQuestionCreation(questionTypeSpinner)
                addNewOption(optionsAdapter, showCheckBox)
            }
            deleteOptionTrashButton.setOnSingleClickListener {
                // Delete the last option when delete is clicked.
                optionsAdapter.submitList(optionsAdapter.currentList.dropLast(1))
            }
            saveButton.saveButtonDisabled()
            saveButton.setOnClickListener {
                saveButton.saveButtonDisabled()
                val title = askAQuestionEditText.text.toString()
                val newQuestionUi = when(questionTypeSpinner.selectedItemPosition){
                    // single, multi, short, long
                    0,1 -> {
                        val items = (optionsListView.adapter as OptionsListAdapter).currentList.map { it.text }
                        if(questionTypeSpinner.selectedItemPosition == 0) {
                            val selectedIndex = optionsAdapter.currentList.indexOfFirst { it.isChecked }
                            val selected = if(selectedIndex == -1)
                                null
                            else
                                selectedIndex
                            QuestionUi.SingleChoiceQuestion(
                                title,
                                items,
                                selected,
                                count++,
                                requiredToAnswer
                            )
                        }
                        else {
                            val selectedIndices = mutableListOf<Int>()
                            optionsAdapter.currentList.forEachIndexed { index, option ->
                                if(option.isChecked)
                                    selectedIndices.add(index)
                            }
                            QuestionUi.MultiChoiceQuestion(
                                title,
                                items,
                                selectedIndices,
                                count++,
                                requiredToAnswer
                            )
                        }
                    }
                    2 -> QuestionUi.ShortAnswer(title, count++, requiredToAnswer)
                    3 -> QuestionUi.LongAnswer(title, count++, requiredToAnswer)
                    else -> null
                }
                // Reset the UI
                askAQuestionEditText.setText("")
                // Save the info
                saveInfo(newQuestionUi!!)
            }
        }
    }

    private fun isMultiOptionQuestionCreation(questionTypeSpinner: Spinner): Boolean {
        return questionTypeSpinner.selectedItemPosition == 1
    }

    private fun addNewOption(optionsAdapter: OptionsListAdapter, showCheckBox: Boolean) {
        optionsAdapter.submitList(optionsAdapter.currentList.plus(Option("", showCheckBox)))
    }

    private fun validateSaveButtonEnabledState(
        optionsAdapter: OptionsListAdapter,
        saveButton: TextView
    ) {
        // Validation criteria
        // No empty answers.
        // At least one answer.
        val isPoll = isPoll()
//        val isMultiOptionCreation = isMultiOptionQuestionCreation(questionTypeSpinner)
        val allOptionsFilledInWithNoBlanks =  (optionsAdapter.currentList.none { it.text.isBlank() } &&
            optionsAdapter.currentList.size > 0)

        val enableButton = if(isPoll) {
            allOptionsFilledInWithNoBlanks
        } else {
            // it's a quiz
            // we have to check if every single choice and multi choice has
            //  at least one answer selected
            allOptionsFilledInWithNoBlanks &&
                    optionsAdapter.currentList.any { it.isChecked }
        }

        if(enableButton)
            saveButton.saveButtonEnabled()
        else
            saveButton.saveButtonDisabled()
    }

    private fun bind(questionUi: QuestionUi.MultiChoiceQuestion) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding){
            questionTitle.text = questionUi.withTitle
            val adapter = ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
//            options.layoutManager = LinearLayoutManager(binding.root.context)
            adapter.addAll(questionUi.options)
            options.adapter = adapter
        }
    }

    private fun bind(questionUi: QuestionUi.SingleChoiceQuestion) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding){
            questionTitle.text = questionUi.withTitle
            val adapter = ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
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