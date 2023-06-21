package live.hms.app2.ui.polls

import android.widget.ArrayAdapter
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.app2.R
import live.hms.app2.databinding.LayoutPollQuestionCreationBinding
import live.hms.app2.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.app2.databinding.LayoutPollQuizItemShortAnswerBinding
import live.hms.app2.util.setOnSingleClickListener

private var count : Long = 0
sealed class QuestionUi(open val index : Long, open val viewType : Int){
    // Makes the creator UI show up first.
    object QuestionCreator : QuestionUi(0, 0)

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(val withTitle: String, val options: List<String>, val correctOptionIndex: Int? = null, override val index : Long) : QuestionUi(index, 1)
    data class SingleChoiceQuestion(val withTitle: String, val options: List<String>, val correctOptionIndex: Int? = null, override val index : Long) : QuestionUi(index, 2)
    data class LongAnswer(val text : String, override val index: Long) : QuestionUi(index, 3)
    data class ShortAnswer(val text : String, override val index: Long) : QuestionUi(index, 4)
}

class PollQuestionViewHolder<T : ViewBinding>(val binding: T,
                                              val saveInfo : (questionUi: QuestionUi) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(questionUi: QuestionUi) {
        when(questionUi) {
            is QuestionUi.QuestionCreator-> bind(QuestionUi.QuestionCreator)
        }
    }

    private fun bind(questionUi: QuestionUi.QuestionCreator) {
        with(binding as LayoutPollQuestionCreationItemBinding) {
            val adapter = ArrayAdapter<String>(binding.root.context, R.layout.layout_poll_quiz_options_item)
            optionsListView.adapter = adapter
            questionTypeSpinner.setOnItemClickListener { parent, view, position, id ->
                // Maybe nothing required.
            }
            addAnOptionTextView.setOnSingleClickListener {
                adapter.add("")
            }
            deleteOptionTrashButton.setOnSingleClickListener {
                // Delete the last option when delete is clicked.
                adapter.remove(adapter.getItem(adapter.count -1 ))
            }
            saveButton.setOnClickListener {
                val title = askAQuestionEditText.text.toString()
                val newQuestionUi = when(questionTypeSpinner.selectedItemPosition){
                    // single, multi, short, long
                    0,1 -> {
                        val items = mutableListOf<String>()
                        for( i in 0 until optionsListView.adapter.count) {
                            items.add(optionsListView.adapter.getItem(i) as String)
                        }
                        if(questionTypeSpinner.selectedItemPosition == 0)
                            QuestionUi.SingleChoiceQuestion(title, items, null,count++)
                        else
                            QuestionUi.MultiChoiceQuestion(title, items, null, count++)
                    }
                    2 -> QuestionUi.ShortAnswer(title, count++)
                    3 -> QuestionUi.LongAnswer(title, count++)
                    else -> null
                }
                saveInfo(newQuestionUi!!)
            }
        }
    }

    private fun bind(questionUi: QuestionUi.ShortAnswer) {
        with(binding as LayoutPollQuizItemShortAnswerBinding) {

        }
    }
}