package live.hms.roomkit.ui.polls.display.voting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.PollStatsQuestions

data class ProgressBarInfo(
    val index : Int,
    val percentage : Int,
    val optionText : String,
    val numberOfVotes : Long
)
class VotingProgressAdapter(val questionIndex : Int) : ListAdapter<ProgressBarInfo, ProgressDisplayViewHolder>(
    DIFFUTIL_CALLBACK
) {
    /**
     * Call this when the votes change, to change the progressbar.
     */
    fun updateProgressBar(pollStatsQuestions: List<PollStatsQuestions>, hmsPoll: HmsPoll) {
        val pollStatsQuestion = pollStatsQuestions.find { it.index == questionIndex.toLong() }
        if(pollStatsQuestion == null)
            return
        // votesForThisOption*100/totalVotes
        val items : List<ProgressBarInfo>? = hmsPoll.questions?.get(pollStatsQuestion.index.toInt() - 1)?.options?.mapIndexed { index, it ->
            val votesForThisOption = pollStatsQuestion.options?.get(index) ?: -1
            ProgressBarInfo(optionText = it.text?:"", numberOfVotes = votesForThisOption, percentage = (votesForThisOption*100/pollStatsQuestion.attemptedTimes).toInt(), index = index)
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