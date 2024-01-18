package live.hms.roomkit.ui.meeting

import android.util.Log
import live.hms.roomkit.ui.polls.display.QuestionContainer

class QuizQuestionTimingUseCase {
    private val questionSeenTimeMap = hashMapOf<String, Long>()
    fun setQuestionStartTime(question: QuestionContainer.Question) {
        val id = getIdForQuestion(question)
        if (!questionSeenTimeMap.contains(id)) {
            Log.d("VerifyAnswer", "Seen $id")
            // save it without overwriting existing values
            questionSeenTimeMap[id] = System.currentTimeMillis()
        }
    }

    fun getQuestionStartTime(question: QuestionContainer.Question): Long?  {
        return questionSeenTimeMap.getOrDefault(getIdForQuestion(question), null)
    }

    private fun getIdForQuestion(question: QuestionContainer.Question): String {
        val pollId = question.poll.pollId
        val questionId = question.question.questionID
        return "$pollId/$questionId"
    }
}