package live.hms.roomkit.ui.polls.display

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayChoicesQuesionBinding
import live.hms.roomkit.databinding.LayoutQuizDisplayShortAnswerBinding
import live.hms.roomkit.ui.polls.display.voting.VotingProgressAdapter
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType

class PollDisplayQuestionHolder<T : ViewBinding>(
    val binding: T,
    private val canRoleViewVotes : Boolean,
    val poll : HmsPoll,
    val saveInfoText: (text : String, position : Int) -> Boolean,
    val saveInfoSingleChoice: (question : HMSPollQuestion, Int?, poll : HmsPoll) -> Boolean,
    val saveInfoMultiChoice: (question : HMSPollQuestion, List<Int>?, poll : HmsPoll) -> Boolean
) : RecyclerView.ViewHolder(binding.root) {

    private val adapter = AnswerOptionsAdapter(canRoleViewVotes)
    var votingProgressAdapter : VotingProgressAdapter? = null

    // There are two different layouts.
    fun bind(question : QuestionContainer) {
        when(question.question.type) {
            HMSPollQuestionType.singleChoice, HMSPollQuestionType.multiChoice -> {
                votingProgressAdapter = VotingProgressAdapter(question.question.questionID)
                optionsBinder(question)
            }
            HMSPollQuestionType.shortAnswer,
            HMSPollQuestionType.longAnswer -> textBinder(question)
        }
    }

    private fun manageVisibility(question : QuestionContainer, binding : LayoutPollsDisplayChoicesQuesionBinding) = with(binding ){
        if(question.voted) {
            votebutton.visibility = View.GONE
            // If results are to be hidden, then don't do the rest of the change that swaps layouts
            if(poll.anonymous && !canRoleViewVotes){
                (options.adapter as AnswerOptionsAdapter).disableOptions()
            } else {
                options.visibility = View.GONE
                votingProgressBars.visibility = View.VISIBLE
                votingProgressBars.adapter = votingProgressAdapter
                val divider =
                    DividerItemDecoration(binding.root.context, RecyclerView.VERTICAL).apply {
                        setDrawable(binding.root.context.getDrawable(R.drawable.polls_display_progress_items_divider)!!)
                    }
                votingProgressBars.addItemDecoration(divider)

                votingProgressBars.layoutManager = LinearLayoutManager(binding.root.context)
            }
            // But nothing will update them. They will always be zero.

        } else {
            options.visibility = View.VISIBLE
            votebutton.visibility = View.VISIBLE
            votingProgressBars.visibility = View.GONE
        }
    }

    private fun optionsBinder(question: QuestionContainer) {
        with(binding as LayoutPollsDisplayChoicesQuesionBinding) {
            manageVisibility(question, this)
            val questionType = when(question.question.type) {
                HMSPollQuestionType.singleChoice -> "SINGLE CHOICE"
                HMSPollQuestionType.multiChoice -> "MULTIPLE CHOICE"
                HMSPollQuestionType.shortAnswer -> "SHORT ANSWER"
                HMSPollQuestionType.longAnswer -> "LONG ANSWER"
            }
            questionNumbering.text = binding.root.resources.getString(R.string.polls_question_numbering_text, question.question.questionID, poll.questions?.size ?:0, questionType)
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
                manageVisibility(question, this)
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