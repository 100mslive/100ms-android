package live.hms.roomkit.ui.polls.display

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayChoicesQuesionBinding
import live.hms.roomkit.databinding.LayoutQuizDisplayShortAnswerBinding
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType

class PollDisplayQuestionHolder<T : ViewBinding>(
    val binding: T,
    val poll : HmsPoll,
    val saveInfoText: (text : String, position : Int) -> Boolean,
    val saveInfoSingleChoice: (question : HMSPollQuestion, Int?, poll : HmsPoll) -> Boolean,
    val saveInfoMultiChoice: (question : HMSPollQuestion, List<Int>?, poll : HmsPoll) -> Boolean
) : RecyclerView.ViewHolder(binding.root) {

    private val adapter = AnswerOptionsAdapter()

    // There are two different layouts.
    fun bind(question : QuestionContainer) {
        when(question.question.type) {
            HMSPollQuestionType.singleChoice, HMSPollQuestionType.multiChoice -> {
                optionsBinder(question)
            }
            HMSPollQuestionType.shortAnswer,
            HMSPollQuestionType.longAnswer -> textBinder(question)
        }
    }

    private fun optionsBinder(question: QuestionContainer) {
        with(binding as LayoutPollsDisplayChoicesQuesionBinding) {
            questionNumbering.text = "Question ${question.question.questionID} of ${poll.questions?.size}"
            questionText.text = question.question.text
            options.layoutManager = LinearLayoutManager(binding.root.context)
            // selected options could be read from the UI directly.
            options.adapter = adapter
            adapter.submitList(question.question.options?.map { Option(it.text?:"", question.question.type == HMSPollQuestionType.multiChoice) })
            votebutton.setOnSingleClickListener {
                val voted : Boolean = if(question.question.type == HMSPollQuestionType.singleChoice){
                    saveInfoSingleChoice(question.question, adapter.getSelectedOptions().firstOrNull(), poll)
                } else if(question.question.type == HMSPollQuestionType.multiChoice) {
                    saveInfoMultiChoice(question.question, adapter.getSelectedOptions(), poll)
                } else {
                    saveInfoText("What?", bindingAdapterPosition)
                }
                Log.d("Poll", "Changed voted to $voted")
                question.voted = voted
            }
        }

    }

    private fun textBinder(question : QuestionContainer) {
        with(binding as LayoutQuizDisplayShortAnswerBinding) {
            textView.text = question.question.text
            votebutton.setOnClickListener {
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    saveInfoText(editText.text.toString(), bindingAdapterPosition)
                }
            }
        }
    }
}