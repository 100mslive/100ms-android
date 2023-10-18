package live.hms.roomkit.ui.polls.display.voting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.PollStatsQuestions
import live.hms.video.polls.models.question.HMSPollQuestion

data class ProgressBarInfo(
    val index : Int,
    val percentage : Int,
    val optionText : String,
    val numberOfVotes : Long,
    val totalVoteCount : Int
)
class VotingProgressAdapter(val questionIndex : Int) : ListAdapter<ProgressBarInfo, ProgressDisplayViewHolder>(
    DIFFUTIL_CALLBACK
) {
    /**
     * Call this when the votes change, to change the progressbar.
     */
    fun updateProgressBar(pollStatsQuestions: List<HMSPollQuestion>, hmsPoll: HmsPoll) {
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
                    optionText = it.text ?: "",
                    numberOfVotes = votesForThisOption,
                    percentage = percentage,
                    index = index,
                    totalVoteCount = pollStatsQuestion.total
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
        val binding = LayoutPollsDisplayResultProgressBarsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProgressDisplayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressDisplayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}