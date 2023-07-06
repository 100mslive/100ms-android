package live.hms.roomkit.ui.polls.display

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import live.hms.roomkit.databinding.LayoutPollsDisplayOptionsItemBinding

class DisplayAnswerOptionsViewHolder(
    val binding: LayoutPollsDisplayOptionsItemBinding,
    getItem: (Int) -> Option,
    ) : ViewHolder(binding.root){

    init {
        if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
            binding.text.setText(getItem(bindingAdapterPosition).text)
        }
        // Both set the same property, only one will be used.
        binding.radioButton.setOnCheckedChangeListener { _, isChecked ->
            getItem(bindingAdapterPosition).apply {
                this.isChecked = isChecked
            }

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
        }
    }
}