package live.hms.roomkit.ui.polls.display

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import live.hms.roomkit.databinding.LayoutPollsDisplayOptionsItemBinding
import live.hms.roomkit.ui.theme.applyTheme
import kotlin.reflect.KProperty0

class DisplayAnswerOptionsViewHolder(
    val binding: LayoutPollsDisplayOptionsItemBinding,
    private val canRoleViewVotes: Boolean,
    getItem: (Int) -> Option,
    setItemSelected: (selected: Int, noOthers: Boolean) -> Unit,
    val answerSelectionUpdated: () -> Unit,
    ) : ViewHolder(binding.root){
    var ignoreCheckChanges = false
    init {
        binding.applyTheme()
        if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
            binding.text.setText(getItem(bindingAdapterPosition).text)
        }
        // Both set the same property, only one will be used.
        binding.radioButton.setOnCheckedChangeListener { _, isChecked ->
            if(!ignoreCheckChanges) {
                if (isChecked) {
                    setItemSelected(bindingAdapterPosition, true)
                    answerSelectionUpdated()
                }
            }
        }

        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if(!ignoreCheckChanges) {
                getItem(bindingAdapterPosition).apply {
                    this.isChecked = isChecked
                }
                answerSelectionUpdated()
            }
        }
    }

    fun bind(option : Option){
        with(binding) {
            ignoreCheckChanges = true
            radioButton.isChecked = option.isChecked
            checkbox.isChecked = option.isChecked
            ignoreCheckChanges = false
            checkbox.visibility = if(option.showCheckbox) View.VISIBLE else View.GONE
            radioButton.visibility = if(option.showCheckbox) View.GONE else View.VISIBLE
            text.setText(option.text)
            if(option.hiddenAndAnswered) {
                radioButton.isEnabled = false
                checkbox.isEnabled = false
            }
        }
    }
}