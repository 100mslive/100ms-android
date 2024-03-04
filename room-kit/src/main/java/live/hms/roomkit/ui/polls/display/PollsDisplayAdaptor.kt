package live.hms.roomkit.ui.polls.display

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutEndPollButtonBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayChoicesQuesionBinding
import live.hms.roomkit.databinding.LayoutQuizDisplayShortAnswerBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType
import live.hms.video.sdk.models.HMSPeer


// Now we decide what the data holder is (let's leave it the original,
//  and also what the viewholder is, will need to create a new one to handle the,
//  logic of answering questions.
// So we'd also need to pass in a function that takes an answer and the question and returns what?
//  could return nothing.
// For a poll, once you vote, you have to show the results of that thing, i.e how many votes it had.

// Also we need not only the question but whether it was answered and some details like the number of votes.
const val EndPollViewType = 10_000
sealed class QuestionContainer {
    abstract fun isQuestion() : Boolean
    abstract fun viewType() : Int
    data class Question(
        val poll: HmsPoll,
        val question: HMSPollQuestion,
        var textAnswers: String? = null,
        var voted: Boolean = false,
    ) : QuestionContainer() {
        override fun isQuestion(): Boolean = true
        override fun viewType(): Int = question.type.ordinal
    }

    object EndPollButton : QuestionContainer() {
        override fun isQuestion(): Boolean = false
        override fun viewType(): Int = EndPollViewType
    }
}
class PollsDisplayAdaptor(
    val localPeer : HMSPeer,
    val getPoll : HmsPoll,
    val saveInfoText : (question: HMSPollQuestion, answer : String, hmsPoll : HmsPoll) -> Boolean,
    val saveInfoSingleChoice : (question : HMSPollQuestion, Int?, hmsPoll : HmsPoll,timeTakenMillis : Long?) -> Boolean,
    val saveInfoMultiChoice : (question : HMSPollQuestion, List<Int>?, hmsPoll : HmsPoll,timeTakenMillis : Long?) -> Boolean,
    val skipped : (question : HMSPollQuestion, poll : HmsPoll) -> Unit,
    val endPoll : (HmsPoll) -> Unit,
    val getQuestionStartTime : (QuestionContainer.Question) -> Long?,
    val setQuestionStartTime : (QuestionContainer.Question) -> Unit,
    val showLeaderBoard : (pollId : String) -> Unit,
) : ListAdapter<QuestionContainer, PollDisplayQuestionHolder<ViewBinding>>(
    DIFFUTIL_CALLBACK
) {
    private val TAG = "PollsDisplayAdaptor"

    private var oldPoll: HmsPoll? = null
    private lateinit var pollId : String
    val updater : MutableList<PollDisplayQuestionHolder<ViewBinding>> = mutableListOf()

    fun displayPoll(hmsPoll: HmsPoll) {
        val questions = hmsPoll.questions?.map { QuestionContainer.Question(hmsPoll, it, voted = it.voted) }
        if(questions != null) {
            submitList(questions)
        }
        this.pollId = hmsPoll.pollId
        this.oldPoll = hmsPoll
    }

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<QuestionContainer>() {
            override fun areItemsTheSame(
                oldItem: QuestionContainer,
                newItem: QuestionContainer
            ) = oldItem.isQuestion() && newItem.isQuestion() &&
                    oldItem is QuestionContainer.Question && newItem is QuestionContainer.Question &&
                    oldItem.question.questionID == newItem.question.questionID

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
            HMSPollQuestionType.singleChoice.ordinal-> LayoutPollsDisplayChoicesQuesionBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
                it.applyTheme()
            }
            HMSPollQuestionType.shortAnswer.ordinal,
            HMSPollQuestionType.longAnswer.ordinal-> LayoutQuizDisplayShortAnswerBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
                it.applyTheme()
            }
            EndPollViewType -> LayoutEndPollButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        val questionHolder = PollDisplayQuestionHolder(view!!,
            canViewResponses(getPoll, localPeer),
            getPoll, ::setTextAnswer, saveInfoSingleChoice, saveInfoMultiChoice,
            skipped, endPoll, canEndPoll(localPeer),
            showLeaderBoard, setQuestionStartTime, getQuestionStartTime,
            itemCount)
        if(viewType == HMSPollQuestionType.multiChoice.ordinal || viewType == HMSPollQuestionType.singleChoice.ordinal) {
            updater.add(questionHolder)
        }
        return questionHolder
    }

    private fun canEndPoll(localPeer: HMSPeer) : Boolean =
        localPeer.hmsRole.permission.pollWrite
    private fun canViewResponses(hmsPoll: HmsPoll, localPeer: HMSPeer): Boolean =
        hmsPoll.rolesThatCanViewResponses.contains(localPeer.hmsRole) || hmsPoll.rolesThatCanViewResponses.isEmpty()

    override fun onBindViewHolder(holder: PollDisplayQuestionHolder<ViewBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).viewType()

    private fun setTextAnswer(answer : String, position: Int): Boolean {
        val option = getItem(position)
        if(option !is QuestionContainer.Question)
            return false
        option.textAnswers = answer
        return saveInfoText(option.question, answer, getPoll)
    }

    fun updatePollVotes(hmsPoll: HmsPoll) {
        if(hmsPoll.pollId != pollId)
            return

        updater.forEach { action ->
            val questions = hmsPoll.questions
            if(questions != null) {
                action.votingProgressAdapter?.updateProgressBar(questions, hmsPoll, canViewResponses(getPoll, localPeer))
            }
        }
    }
    fun getItemForPosition(position: Int): QuestionContainer = getItem(position)
}