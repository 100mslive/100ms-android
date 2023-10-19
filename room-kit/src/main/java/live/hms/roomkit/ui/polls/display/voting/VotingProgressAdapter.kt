package live.hms.roomkit.ui.polls.display.voting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayResultQuizAnswerItemsBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.HmsPollCategory
import live.hms.video.polls.models.HmsPollState
import live.hms.video.polls.models.answer.HMSPollQuestionAnswer
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType

data class ProgressBarInfo(
    val category: HmsPollCategory,
    val index: Int,
    val percentage: Int,
    val optionText: String,
    val numberOfVotes: Long,
    val totalVoteCount: Int,
    val pollQuestionAnswer: HMSPollQuestionAnswer?,
    val questionType: HMSPollQuestionType?,
    val pollState : HmsPollState,
    val hideVoteCount : Boolean
)
class VotingProgressAdapter(val questionIndex : Int) : ListAdapter<ProgressBarInfo, ProgressDisplayViewHolder>(
    DIFFUTIL_CALLBACK
) {
    /**
     * Call this when the votes change, to change the progressbar.
     */
    fun updateProgressBar(pollStatsQuestions: List<HMSPollQuestion>, hmsPoll: HmsPoll, canViewResponses : Boolean) {
        val pollStatsQuestion = pollStatsQuestions.find { it.questionID == questionIndex }
        if(pollStatsQuestion == null)
            return

        // votesForThisOption*100/totalVotes
        val items: List<ProgressBarInfo>? =
            hmsPoll.questions?.get(pollStatsQuestion.questionID - 1)?.options?.mapIndexed { index, it ->
                val votesForThisOption = pollStatsQuestion.options?.get(index)?.voteCount ?: -1

                val percentage : Int = if (pollStatsQuestion.total == 0) {
                    100
                } else {
                    (votesForThisOption * 100 / pollStatsQuestion.total).toInt()
                }
                ProgressBarInfo(
                    category =  hmsPoll.category,
                    optionText = it.text ?: "",
                    numberOfVotes = votesForThisOption,
                    percentage = percentage,
                    index = index,
                    totalVoteCount = pollStatsQuestion.total,
                    pollQuestionAnswer = hmsPoll.questions?.get(pollStatsQuestion.questionID - 1)?.correctAnswer,
                    questionType = hmsPoll.questions?.get(pollStatsQuestion.questionID - 1)?.type,
                    pollState = hmsPoll.state,
                    hideVoteCount = !canViewResponses
                )
        }
        submitList(items)
    }

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<ProgressBarInfo>() {
            override fun areItemsTheSame(
                oldItem: ProgressBarInfo,
                newItem: ProgressBarInfo
            ) = oldItem.index == newItem.index

            override fun areContentsTheSame(
                oldItem: ProgressBarInfo,
                newItem: ProgressBarInfo
            ) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProgressDisplayViewHolder {
        val binding = when(viewType) {
            HmsPollCategory.POLL.ordinal -> LayoutPollsDisplayResultProgressBarsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HmsPollCategory.QUIZ.ordinal -> LayoutPollsDisplayResultQuizAnswerItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> null
        }
        return ProgressDisplayViewHolder(binding!!)
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        return getItem(position).category.ordinal
    }

    override fun onBindViewHolder(holder: ProgressDisplayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}