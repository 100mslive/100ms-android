package live.hms.roomkit.ui.polls

sealed class QuestionUi(
    open var index: Long,
    open val viewType: Int
) {
    // Makes the creator UI show up first.
    data class QuestionCreator(var currentQuestion: ChoiceQuestions = SingleChoiceQuestion(),
                               var isPoll : Boolean = true,
                               var requiredToAnswer: Boolean = false,
    ) : QuestionUi(0, 0)

    sealed class ChoiceQuestions(
        override var index: Long,
        override val viewType: Int,
        open var requiredToAnswer: Boolean,
        open var withTitle: String = "",
        open var options: List<String> = emptyList(),
        open var selections : List<Int> = emptyList(),
    ) : QuestionUi(index, viewType){
        open fun isValid(isPoll : Boolean) : Boolean {
            return withTitle.isNotEmpty() &&
                    options.isNotEmpty() &&
                    options.none { it.isEmpty()}
        }
    }

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: List<Int>? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false
    ) : ChoiceQuestions(index, 1, requiredToAnswer) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || correctOptionIndex != null)
        }
    }

    data class SingleChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: Int? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false
    ) : ChoiceQuestions(index, 2, requiredToAnswer) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || correctOptionIndex != null)
        }
    }

    data class LongAnswer(
        val text: String, override var index: Long,
    ) : QuestionUi(index, 3)

    data class ShortAnswer(
        val text: String, override var index: Long,
    ) : QuestionUi(index, 4)

    object AddAnotherItemView : QuestionUi(-1, 5)
    object LaunchButton : QuestionUi(-2, 6)
}