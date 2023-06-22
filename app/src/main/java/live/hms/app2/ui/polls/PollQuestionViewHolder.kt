package live.hms.app2.ui.polls

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.app2.R
import live.hms.app2.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.app2.databinding.LayoutPollQuizItemShortAnswerBinding
import live.hms.app2.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding
import live.hms.app2.util.setOnSingleClickListener

private var count : Long = 0
sealed class QuestionUi(open val index : Long, open val viewType : Int, open val requiredToAnswer : Boolean){
    // Makes the creator UI show up first.
    object QuestionCreator : QuestionUi(0, 0, false)

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(val withTitle: String, val options: List<String>, val correctOptionIndex: Int? = null, override val index : Long,
                                   override val requiredToAnswer: Boolean) : QuestionUi(index, 1, requiredToAnswer)
    data class SingleChoiceQuestion(val withTitle: String,
                                    val options: List<String>,
                                    val correctOptionIndex: Int? = null,
                                    override val index : Long,
                                    override val requiredToAnswer: Boolean) : QuestionUi(index, 2, requiredToAnswer)
    data class LongAnswer(val text : String, override val index: Long,
                          override val requiredToAnswer: Boolean) : QuestionUi(index, 3, requiredToAnswer)
    data class ShortAnswer(val text : String, override val index: Long,
                           override val requiredToAnswer: Boolean) : QuestionUi(index, 4, requiredToAnswer)
}

class PollQuestionViewHolder<T : ViewBinding>(val binding: T,
                                              val saveInfo : (questionUi: QuestionUi) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private val TAG = "PollQuestionViewHolder"

    fun bind(questionUi: QuestionUi) {
        when(questionUi) {
            is QuestionUi.QuestionCreator-> bind(QuestionUi.QuestionCreator)
            is QuestionUi.LongAnswer -> bind(questionUi)
            is QuestionUi.MultiChoiceQuestion -> bind(questionUi)
            is QuestionUi.ShortAnswer -> bind(questionUi)
            is QuestionUi.SingleChoiceQuestion -> bind(questionUi)
        }
    }


    private fun bind(questionUi: QuestionUi.QuestionCreator) {
        with(binding as LayoutPollQuestionCreationItemBinding) {
            val requiredToAnswer = !notRequiredToAnswer.isChecked


//            val adapter = object : ArrayAdapter<String>(binding.root.context, R.layout.layout_poll_quiz_options_item) {
//                override fun getView(
//                    position: Int, convertView: View?,
//                    parent: ViewGroup
//                ): View {
//                    val view = super.getView(position, convertView, parent)
//                    Log.d(TAG,"Looking into position $position, hasView: ${view == null}")
//
//                    if(view.tag == null) {
//                        (view as EditText).addTextChangedListener {
//                            // Modify the text for the given position and then
//                            //  take it into account when you're changing the number
//                            //  of items in the adapter.
//                            options[position] = it.toString()
//                            Log.d(TAG, "Edited $options")
//                        }
////                       if(!hasFocus)
////                       {
////                           val newText = (v as EditText).text.toString()
////                           remove(getItem(position))
////                           add(newText)
////                       }
////                    }
//                        view.tag = position
//                    }
//                    return view
//                }
//
//            }

            val optionsAdapter = OptionsListAdapter()
            optionsListView.adapter = optionsAdapter
            optionsListView.layoutManager = LinearLayoutManager(binding.root.context)
            questionTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    optionsAdapter.submitList(emptyList())
                    // Only the UI might need to be toggled
                    Log.d(TAG,"Toggle UI")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
            addAnOptionTextView.setOnSingleClickListener {
                val showCheckBox = questionTypeSpinner.selectedItemPosition == 1
                optionsAdapter.submitList(optionsAdapter.currentList.plus(Option("", showCheckBox)))
                // Clear the adaptor
//                adapter.clear()
//                // Add an item to it.
//                Log.d(TAG,"Adding $options")
//                options[options.keys.size]=""
//                Log.d(TAG,"Is now $options")
//                // Set the options to the adaptor
//                adapter.addAll(options.values.toList())
            }
            deleteOptionTrashButton.setOnSingleClickListener {
                // Delete the last option when delete is clicked.
                optionsAdapter.submitList(optionsAdapter.currentList.dropLast(1))
//                adapter.remove(options.remove(adapter.count -1))
            }
            saveButton.setOnClickListener {
                val title = askAQuestionEditText.text.toString()
                val newQuestionUi = when(questionTypeSpinner.selectedItemPosition){
                    // single, multi, short, long
                    0,1 -> {
                        val items = (optionsListView.adapter as OptionsListAdapter).currentList.map { it.text }
                        if(questionTypeSpinner.selectedItemPosition == 0)
                            QuestionUi.SingleChoiceQuestion(title, items, null,count++, requiredToAnswer)
                        else
                            QuestionUi.MultiChoiceQuestion(title, items, null, count++, requiredToAnswer)
                    }
                    2 -> QuestionUi.ShortAnswer(title, count++, requiredToAnswer)
                    3 -> QuestionUi.LongAnswer(title, count++, requiredToAnswer)
                    else -> null
                }
                saveInfo(newQuestionUi!!)
            }
        }
    }

    private fun bind(questionUi: QuestionUi.MultiChoiceQuestion) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding){
            questionTitle.text = questionUi.withTitle
            val adapter = ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
            adapter.addAll(questionUi.options)
        }
    }

    private fun bind(questionUi: QuestionUi.SingleChoiceQuestion) {
        with(binding as LayoutPollQuizOptionsItemMultiChoiceBinding){
            questionTitle.text = questionUi.withTitle
            val adapter = ArrayAdapter<String>(binding.root.context, android.R.layout.simple_list_item_1)
            adapter.addAll(questionUi.options)
        }
    }

    private fun bind(questionUi: QuestionUi.ShortAnswer) {
        with(binding as LayoutPollQuizItemShortAnswerBinding) {
            questionType.text = "Short Answer"
            questionText.text = questionUi.text
        }
    }
    private fun bind(questionUi: QuestionUi.LongAnswer) {
        with(binding as LayoutPollQuizItemShortAnswerBinding) {
            questionType.text = "Long Answer"
            questionText.text = questionUi.text
        }
    }
}