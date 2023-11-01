package live.hms.roomkit.ui.polls

class QuestionToOptions {
    private fun questionToOptions(question: QuestionUi.SingleChoiceQuestion, isPoll : Boolean): List<Option>  {
        val selectedOptionIndex = question.selections.firstOrNull()
        return question.options.mapIndexed { int, text ->
            Option(text, if(isPoll) null else false, int == selectedOptionIndex)
        }
    }
    private fun questionToOptions(question: QuestionUi.MultiChoiceQuestion, isPoll : Boolean): List<Option>  {
        val selectedOptionIndex = question.selections
        return question.options.mapIndexed { int, text ->
            Option(text, if(isPoll) null else true, int in selectedOptionIndex)
        }
    }
    fun questionToOptions(question: QuestionUi.ChoiceQuestions?, isPoll : Boolean) : List<Option>? =
        when (question?.viewType) {
            1 -> questionToOptions(question as QuestionUi.MultiChoiceQuestion, isPoll)
            2 -> questionToOptions(question as QuestionUi.SingleChoiceQuestion, isPoll)
            else -> null
        }
}