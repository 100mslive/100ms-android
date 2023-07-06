package live.hms.roomkit.ui.polls.display

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutPollQuestionCreationItemBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayChoicesQuesionBinding
import live.hms.roomkit.databinding.LayoutQuizDisplayShortAnswerBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType

// Now we decide what the data holder is (let's leave it the original,
//  and also what the viewholder is, will need to create a new one to handle the,
//  logic of answering questions.
// So we'd also need to pass in a function that takes an answer and the question and returns what?
//  could return nothing.
// For a poll, once you vote, you have to show the results of that thing, i.e how many votes it had.

// Also we need not only the question but whether it was answered and some details like the number of votes.
data class QuestionContainer(
    val question: HMSPollQuestion,
    var textAnswers : String? = null,
    var voted : Boolean = false
)
class PollsDisplayAdaptor(
    val saveInfoText : (question: HMSPollQuestion, answer : String) -> Boolean,
    val saveInfoSingleChoice : (question : HMSPollQuestion, Int?) -> Boolean,
    val saveInfoMultiChoice : (question : HMSPollQuestion, List<Int>?) -> Boolean
) : ListAdapter<QuestionContainer, PollDisplayQuestionHolder<ViewBinding>>(
    DIFFUTIL_CALLBACK
) {

    private lateinit var poll: HmsPoll

    fun displayPoll(hmsPoll: HmsPoll) {
        val questions = hmsPoll.questions?.map { QuestionContainer(it) }
        Log.d("PollsDisplay","Que $questions")
        if(questions != null) {
            submitList(questions)
        }
        this.poll = hmsPoll
    }

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<QuestionContainer>() {
            override fun areItemsTheSame(
                oldItem: QuestionContainer,
                newItem: QuestionContainer
            ) = oldItem.question.questionID == newItem.question.questionID

            override fun areContentsTheSame(
                oldItem: QuestionContainer,
                newItem: QuestionContainer
            ) = oldItem == newItem
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PollDisplayQuestionHolder<ViewBinding> {
        val view = when(viewType) {
            HMSPollQuestionType.multiChoice.ordinal,
            HMSPollQuestionType.singleChoice.ordinal-> LayoutPollsDisplayChoicesQuesionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HMSPollQuestionType.shortAnswer.ordinal,
            HMSPollQuestionType.longAnswer.ordinal-> LayoutQuizDisplayShortAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        return PollDisplayQuestionHolder(view!!, poll, ::setTextAnswer, saveInfoSingleChoice, saveInfoMultiChoice)
    }

    override fun onBindViewHolder(holder: PollDisplayQuestionHolder<ViewBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).question.type.ordinal

    private fun setTextAnswer(answer : String, position: Int): Boolean {
        val option = getItem(position)
        option.textAnswers = answer
        return saveInfoText(option.question, answer)
    }
}