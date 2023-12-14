package live.hms.roomkit.ui.polls

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutMultiChoiceQuestionOptionItemBinding
import live.hms.roomkit.ui.theme.applyTheme

class MultiChoiceQuestionOptionItem(
    question: QuestionUi.ChoiceQuestions,
    val option: String
) : BindableItem<LayoutMultiChoiceQuestionOptionItemBinding>(question.getItemId()) {
    override fun bind(viewBinding: LayoutMultiChoiceQuestionOptionItemBinding, position: Int) {
        with(viewBinding) {
            applyTheme()
            optionText.text = option
        }
        // We need a divider of spacing 1
    }

    override fun getLayout(): Int = R.layout.layout_multi_choice_question_option_item

    override fun initializeViewBinding(view: View): LayoutMultiChoiceQuestionOptionItemBinding =
        LayoutMultiChoiceQuestionOptionItemBinding.bind(view)

}