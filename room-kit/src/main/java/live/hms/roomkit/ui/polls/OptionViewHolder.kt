package live.hms.roomkit.ui.polls

import android.view.View
import android.widget.ListAdapter
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemBinding
import live.hms.roomkit.setOnSingleClickListener

class OptionViewHolder(val binding : LayoutPollQuizOptionsItemBinding,
                       getItem: (position : Int) -> Option,
                       selectRadioOption : (Int) -> Unit,
                       selectCheckboxOption : (Int, Boolean) -> Unit,
                       deleteThisOption : (position : Int) -> Unit,
                       ) : ViewHolder(binding.root) {
   var skipCheckChange = false
    init {
        if(bindingAdapterPosition != NO_POSITION) {
            binding.text.setText(getItem(bindingAdapterPosition).text)
        }
        binding.deleteOptionTrashButton.setOnSingleClickListener {
            // Avoid edittext crash
            binding.root.requestFocus()
            deleteThisOption(bindingAdapterPosition)
        }
        binding.text.doOnTextChanged { text, _, _, _ ->
            getItem(bindingAdapterPosition).text = text.toString()
        }
        // Both set the same property, only one will be used.
        binding.radioButton.setOnCheckedChangeListener { _, isChecked ->
            // Radio buttons reset all others when selected
            if(skipCheckChange)
                return@setOnCheckedChangeListener
            if(isChecked)
                selectRadioOption(bindingAdapterPosition)
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if(skipCheckChange)
                return@setOnCheckedChangeListener
            selectCheckboxOption(bindingAdapterPosition, isChecked)
        }
    }

    fun bind(option : Option) {
        skipCheckChange = true
        binding.radioButton.isChecked = option.isChecked
        binding.checkbox.isChecked = option.isChecked
        skipCheckChange = false
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