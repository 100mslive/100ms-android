package live.hms.roomkit.ui.polls.display.voting

import android.os.Build
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayResultProgressBarsItemBinding
import live.hms.roomkit.databinding.LayoutPollsDisplayResultQuizAnswerItemsBinding
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.polls.models.HmsPollCategory
import live.hms.video.polls.models.HmsPollState
import live.hms.video.polls.models.question.HMSPollQuestionType

class ProgressDisplayViewHolder(
    val binding: ViewBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ProgressBarInfo) {
        if (item.category == HmsPollCategory.POLL) {
            pollsBinding(binding as LayoutPollsDisplayResultProgressBarsItemBinding, item)
        } else {
            quizBinding(binding as LayoutPollsDisplayResultQuizAnswerItemsBinding, item)
        }
    }

    private fun quizBinding(
        binding: LayoutPollsDisplayResultQuizAnswerItemsBinding,
        item: ProgressBarInfo
    ) = with(binding) {
        applyTheme()
        optionText.text = item.optionText
        val numVotes = item.numberOfVotes.toInt()
        val isAnswerCorrect = when (item.questionType) {
            HMSPollQuestionType.singleChoice -> {
                item.pollQuestionAnswer?.option == item.index + 1
            }

            HMSPollQuestionType.multiChoice -> {
                item.pollQuestionAnswer?.options?.contains(item.index + 1) == true
            }

            else -> false
        }
        if (isAnswerCorrect)
            optionText.drawableStart =
                ResourcesCompat.getDrawable(root.resources, R.drawable.circle_tick, null)?.apply {
                    setTint(
                        getColorOrDefault(
                            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                        )
                    )
                }

        peopleAnswering.text = if(item.pollState == HmsPollState.STOPPED) {
            binding.root.resources.getQuantityString(
                R.plurals.poll_quiz_results_for_ended_polls,
                numVotes,
                numVotes,
                item.percentage)
        }
            else {
                binding.root.resources.getQuantityString(
                R.plurals.poll_quiz_results,
                numVotes,
                numVotes
            )
        }
    }

    private fun pollsBinding(
        binding: LayoutPollsDisplayResultProgressBarsItemBinding,
        item: ProgressBarInfo
    ) {
        with(binding) {
            applyTheme()
            if (item.totalVoteCount == 0) {
                questionProgressBar.visibility = View.GONE
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                questionProgressBar.setProgress(item.percentage, true)
            }
            answer.text = item.optionText
            val numVotes = item.numberOfVotes.toInt()
            totalVotes.text =
                binding.root.resources.getQuantityString(R.plurals.poll_votes, numVotes, numVotes)
        }
    }

}