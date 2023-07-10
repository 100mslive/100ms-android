package live.hms.roomkit.ui.polls.display.voting

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.PollStatsQuestions

class ProgressDisplayViewHolder(
    val binding: LayoutPollsDisplayResultProgressBarsItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ProgressBarInfo) {
        with(binding) {
            questionProgressBar.setProgress(item.percentage, true)
            answer.text = item.optionText
            totalVotes.text = item.numberOfVotes.toString()
        }
    }

}