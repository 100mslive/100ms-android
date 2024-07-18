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
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault
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
                item.pollQuestionAnswer?.option == item.index
            }

            HMSPollQuestionType.multiChoice -> {
                item.pollQuestionAnswer?.options?.contains(item.index) == true
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
            val myAnswer = when (item.questionType) {
                HMSPollQuestionType.singleChoice -> {
                    item.myAnswer?.selectedOption == item.index
                }

                HMSPollQuestionType.multiChoice -> {
                    item.myAnswer?.selectedOptions?.contains(item.index) == true
                }
                else -> false
            }
            if( myAnswer )
                binding.root.resources.getString(R.string.quiz_your_answer)
            else
                ""
//            binding.root.resources.getQuantityString(
//                R.plurals.poll_quiz_results_for_ended_polls,
//                numVotes,
//                numVotes,
//                item.percentage)
        }
            else {
                binding.root.resources.getQuantityString(
                R.plurals.poll_quiz_results,
                numVotes,
                numVotes
            )
        }
        if (item.hideVoteCount && item.pollState != HmsPollState.STOPPED)
            peopleAnswering.visibility = View.GONE
        else
            peopleAnswering.visibility = View.VISIBLE
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
            if (item.hideVoteCount) {
                questionProgressBar.visibility = View.GONE
                totalVotes.visibility = View.GONE
            }
            else {
                questionProgressBar.visibility = View.VISIBLE
                totalVotes.visibility = View.VISIBLE
            }
        }
    }

}