package live.hms.roomkit.ui.polls.display

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPollsDisplayChoicesQuesionBinding
import live.hms.roomkit.databinding.LayoutQuizDisplayShortAnswerBinding
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.polls.display.voting.VotingProgressAdapter
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.highlightCorrectAnswer
import live.hms.roomkit.ui.theme.voteButtons
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.HmsPollCategory
import live.hms.video.polls.models.HmsPollState
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType

class PollDisplayQuestionHolder<T : ViewBinding>(
    val binding: T,
    private val canRoleViewVotes : Boolean,
    val poll : HmsPoll,
    val saveInfoText: (text : String, position : Int) -> Boolean,
    val saveInfoSingleChoice: (question : HMSPollQuestion, Int?, poll : HmsPoll) -> Boolean,
    val saveInfoMultiChoice: (question : HMSPollQuestion, List<Int>?, poll : HmsPoll) -> Boolean,
    // This isn't implemented yet
    val skipped : (question : HMSPollQuestion, poll : HmsPoll) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val adapter = AnswerOptionsAdapter(canRoleViewVotes) { answersSelected ->
        if(binding is LayoutPollsDisplayChoicesQuesionBinding){
            binding.votebutton.isEnabled = answersSelected
        }
    }
    var votingProgressAdapter : VotingProgressAdapter? = null

    // There are two different layouts.
    fun bind(question : QuestionContainer) {
        when(question.question.type) {
            HMSPollQuestionType.singleChoice, HMSPollQuestionType.multiChoice -> {
                votingProgressAdapter = VotingProgressAdapter(question.question.questionID)
                    .apply {
                        updateProgressBar(poll.questions ?: emptyList(), poll, canRoleViewVotes)
                    }

                optionsBinder(question)
            }
            HMSPollQuestionType.shortAnswer,
            HMSPollQuestionType.longAnswer -> textBinder(question)
        }
    }

    private fun manageVisibility(question : QuestionContainer, binding : LayoutPollsDisplayChoicesQuesionBinding) = with(binding ){
        // This is now also different if it's polls or quiz.
        // For polls we see answers immediately and they are updated.
        // For quizzes, we do not see the answers.
        if(poll.state == HmsPollState.STOPPED && poll.category == HmsPollCategory.QUIZ) {
            root.highlightCorrectAnswer(isQuestionCorrectlyAnswered(question))
        }
        if(question.voted || poll.state == HmsPollState.STOPPED) {
            if(!question.voted) {
                votebutton.visibility = View.GONE
            } else {
                votebutton.isEnabled = false
                votebutton.background = null
                votebutton.setTextColor(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                        HMSPrebuiltTheme.getDefaults().onsurface_low_emp
                    )
                )
                votebutton.text = if(poll.category == HmsPollCategory.QUIZ) binding.root.resources.getString(R.string.polls_quiz_answer_button_complete) else binding.root.resources.getString(R.string.polls_answer_button_complete)
            }

            // If results are to be hidden, then don't do the rest of the change that swaps layouts
            if(poll.anonymous && !canRoleViewVotes){
                (options.adapter as AnswerOptionsAdapter?)?.disableOptions()
            } else {
                if( poll.category == HmsPollCategory.POLL || ( poll.category == HmsPollCategory.QUIZ && poll.state == HmsPollState.STOPPED)) {
                    if(poll.category == HmsPollCategory.QUIZ) {
                        val correctlyAnswered = isQuestionCorrectlyAnswered(question)
                        val correctnessIndicator =
                            if (correctlyAnswered) R.drawable.correct_answer_quiz_icon
                            else R.drawable.wrong_answer_quiz_icon

                        val indicatorColor = if (correctlyAnswered) {
                            getColorOrDefault(
                                HMSPrebuiltTheme.getColours()?.alertSuccess,
                                HMSPrebuiltTheme.getDefaults().error_default
                            )
                        } else {
                            getColorOrDefault(
                                HMSPrebuiltTheme.getColours()?.alertErrorDefault,
                                HMSPrebuiltTheme.getDefaults().error_default
                            )
                        }

                        questionNumbering.drawableStart = AppCompatResources.getDrawable(
                            binding.root.context, correctnessIndicator
                        )?.apply { setTint(indicatorColor) }
                        questionNumbering.setTextColor(indicatorColor)
                    }
                    options.visibility = View.GONE
                    votingProgressBars.visibility = View.VISIBLE
                    votingProgressBars.adapter = votingProgressAdapter
                    if(poll.category == HmsPollCategory.POLL) {
                        val divider =
                            DividerItemDecoration(
                                binding.root.context,
                                RecyclerView.VERTICAL
                            ).apply {
                                setDrawable(binding.root.context.getDrawable(R.drawable.polls_display_progress_items_divider)!!)
                            }
                        votingProgressBars.addItemDecoration(divider)
                    }

                    votingProgressBars.layoutManager = LinearLayoutManager(binding.root.context)
                    // Hide vote button for stopped quizzes
                    votebutton.visibility = View.GONE
                } else {
                    (options.adapter as AnswerOptionsAdapter?)?.disableOptions()
                }
            }
            // But nothing will update them. They will always be zero.

        } else {
            votebutton.visibility = View.VISIBLE
            options.visibility = View.VISIBLE
            votebutton.isEnabled = adapter.getSelectedOptions().isNotEmpty()
            votingProgressBars.visibility = View.GONE
            votebutton.text = if(poll.category == HmsPollCategory.QUIZ) binding.root.resources.getString(R.string.polls_quiz_answer_button) else binding.root.resources.getString(R.string.polls_answer_button)
            votebutton.voteButtons()
        }
//        skipButton.visibility = if(question.question.canSkip) View.VISIBLE else View.GONE
    }

    private fun isQuestionCorrectlyAnswered(questionContainer: QuestionContainer): Boolean {
        val question = questionContainer.question
        val isAnswerCorrect = when (question.type) {
            HMSPollQuestionType.singleChoice -> {
                val myAnswer = question.myResponses.find { it.questionId == question.questionID }?.selectedOption
                question.correctAnswer?.option == myAnswer
            }

            HMSPollQuestionType.multiChoice -> {
                val myAnswer = question.myResponses.find { it.questionId == question.questionID }?.selectedOptions
                val correctOptions = question.correctAnswer?.options
                return if(myAnswer == null || correctOptions == null)
                    false
                else
                    myAnswer.containsAll(correctOptions) && myAnswer.size == correctOptions.size
            }
            else -> false
        }
        return isAnswerCorrect
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
            adapter.submitList(question.question.options?.map { option ->
                Option(option.text ?: "",
                    question.question.type == HMSPollQuestionType.multiChoice,
                    isChecked = question.question.myResponses.find {
                        if(question.question.type == HMSPollQuestionType.multiChoice) {
                            it.selectedOptions?.contains(option.index) == true
                        } else {
                            it.selectedOption == option.index
                        }
                    } != null,
                    hiddenAndAnswered = question.voted
                )
            })
            skipButton.setOnSingleClickListener {
                // TODO skip

            }
            votebutton.setOnSingleClickListener {
                val voted : Boolean = if(question.question.type == HMSPollQuestionType.singleChoice){
                    saveInfoSingleChoice(question.question, adapter.getSelectedOptions().firstOrNull(), poll)
                } else if(question.question.type == HMSPollQuestionType.multiChoice) {
                    saveInfoMultiChoice(question.question, adapter.getSelectedOptions(), poll)
                } else {
                    saveInfoText("What?", bindingAdapterPosition)
                }
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