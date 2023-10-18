package live.hms.roomkit.ui.polls.display.voting

import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.PollStatsQuestions

class ProgressDisplayViewHolder(
    val binding: LayoutPollsDisplayResultProgressBarsItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ProgressBarInfo) {
        with(binding) {
            if(item.totalVoteCount == 0) {
                questionProgressBar.visibility = View.GONE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                questionProgressBar.setProgress(item.percentage, true)
            }
            answer.text = item.optionText
            val numVotes = item.numberOfVotes.toInt()
            totalVotes.text = binding.root.resources.getQuantityString(R.plurals.poll_votes, numVotes, numVotes)
        }
    }

}