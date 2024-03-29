package live.hms.roomkit.ui.polls

sealed class QuestionUi(
    open var index: Long,
    open val viewType: Int
) {
    abstract fun getItemId() : Long
    // Makes the creator UI show up first.
    data class QuestionCreator(var currentQuestion: ChoiceQuestions = SingleChoiceQuestion(),
                               var isPoll : Boolean = true,
                               var requiredToAnswer: Boolean = false,
        val questionInEditIndex : Long? = null
    ) : QuestionUi(0, 0) {
        override fun getItemId(): Long = viewType.toLong() // It's always first and there's only one
    }

    sealed class ChoiceQuestions(
        override var index: Long,
        override val viewType: Int,
        open var requiredToAnswer: Boolean,
        open var withTitle: String = "",
        open var options: List<String> = emptyList(),
        open var selections : List<Int> = emptyList(),
        open var totalQuestions : Long
    ) : QuestionUi(index, viewType){
        open fun isValid(isPoll : Boolean) : Boolean {
            return withTitle.isNotEmpty() &&
                    options.isNotEmpty() &&
                    options.none { it.isEmpty()}
        }

        override fun getItemId(): Long = 1000 + index
    }

    // Actual questions that might be asked.
    data class MultiChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: List<Int>? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false,
        override var totalQuestions: Long = 0,
        override var selections: List<Int> = emptyList()
    ) : ChoiceQuestions(index, 1, requiredToAnswer, totalQuestions = totalQuestions, selections = selections) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || selections.isNotEmpty())
        }
    }

    data class SingleChoiceQuestion(
        override var withTitle: String = "",
        override var options: List<String> = listOf("",""),
        var correctOptionIndex: Int? = null,
        override var index: Long = -1,
        override var requiredToAnswer: Boolean = false,
        override var totalQuestions: Long = 0,
        override var selections : List<Int> = emptyList()
    ) : ChoiceQuestions(index, 2, requiredToAnswer, totalQuestions = totalQuestions, selections = selections) {
        override fun isValid(isPoll : Boolean): Boolean {
            return super.isValid(isPoll) && (isPoll || selections.isNotEmpty())
        }
    }

//    data class LongAnswer(
//        val text: String, override var index: Long,
//    ) : QuestionUi(index, 3){
//        override fun getItemId(): Long = TODO()
//    }
//
//    data class ShortAnswer(
//        val text: String, override var index: Long,
//    ) : QuestionUi(index, 4) {
//        override fun getItemId(): Long = TODO()
//    }

    object AddAnotherItemView : QuestionUi(-1, 5){
        override fun getItemId(): Long = viewType.toLong() + 2000
    }
    data class LaunchButton(val isPoll : Boolean, var enabled : Boolean) : QuestionUi(-2, 6){
        override fun getItemId(): Long = viewType.toLong() + 2000
    }
}