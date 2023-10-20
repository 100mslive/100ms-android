package live.hms.roomkit.ui.polls

import android.view.View
import android.widget.ListAdapter
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemBinding

class OptionViewHolder(val binding : LayoutPollQuizOptionsItemBinding,
                       getItem: (position : Int) -> Option,
                       selectOnlyCurrentPosition : (Int) -> Unit,
                       refreshSubmitButton : () -> Unit) : ViewHolder(binding.root) {
    init {
        if(bindingAdapterPosition != NO_POSITION) {
            binding.text.setText(getItem(bindingAdapterPosition).text)
        }
        binding.text.doOnTextChanged { text, _, _, _ ->
            getItem(bindingAdapterPosition).text = text.toString()
        }
        // Both set the same property, only one will be used.
        binding.radioButton.setOnCheckedChangeListener { _, isChecked ->
            // Radio buttons reset all others when selected
            if(isChecked)
                selectOnlyCurrentPosition(bindingAdapterPosition)
            refreshSubmitButton()
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            getItem(bindingAdapterPosition).isChecked = isChecked
            refreshSubmitButton()
        }
    }
    fun bind(option : Option) {
        binding.radioButton.isChecked = option.isChecked
        binding.checkbox.isChecked = option.isChecked
        when(option.showCheckbox) {
            true -> {
                binding.checkbox.visibility = View.VISIBLE
                binding.radioButton.visibility = View.GONE
            }
            false -> {
                binding.radioButton.visibility = View.VISIBLE
                binding.checkbox.visibility = View.GONE
            }
            null -> {
                binding.checkbox.visibility = View.GONE
                binding.radioButton.visibility = View.GONE
            }
        }
        binding.text.setText(option.text)
    }
}