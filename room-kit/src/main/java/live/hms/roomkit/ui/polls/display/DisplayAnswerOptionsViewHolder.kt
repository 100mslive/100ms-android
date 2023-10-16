package live.hms.roomkit.ui.polls.display

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import live.hms.roomkit.databinding.LayoutPollsDisplayOptionsItemBinding
import live.hms.roomkit.ui.theme.applyTheme

class DisplayAnswerOptionsViewHolder(
    val binding: LayoutPollsDisplayOptionsItemBinding,
    private val canRoleViewVotes : Boolean,
    getItem: (Int) -> Option,
    setItemSelected: (selected : Int, noOthers: Boolean) -> Unit,
    ) : ViewHolder(binding.root){

    fun q() {
        binding.radioButton.isChecked = false
    }

    init {
        binding.applyTheme()
        if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
            binding.text.setText(getItem(bindingAdapterPosition).text)
        }
        // Both set the same property, only one will be used.
        binding.radioButton.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked)
                setItemSelected(bindingAdapterPosition, true)
        }

        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            getItem(bindingAdapterPosition).apply {
                this.isChecked = isChecked
            }
        }
    }

    fun bind(option : Option){
        with(binding) {
            radioButton.isChecked = option.isChecked
            checkbox.isChecked = option.isChecked
            checkbox.visibility = if(option.showCheckbox) View.VISIBLE else View.GONE
            radioButton.visibility = if(option.showCheckbox) View.GONE else View.VISIBLE
            text.setText(option.text)
            if(option.hiddenAndAnswered && !canRoleViewVotes) {
                radioButton.isEnabled = false
                checkbox.isEnabled = false
            }
        }
    }
}