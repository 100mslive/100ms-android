package live.hms.roomkit.ui.polls

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemBinding
import live.hms.roomkit.databinding.LayoutPollQuizOptionsItemMultiChoiceBinding
import live.hms.roomkit.ui.theme.setTheme
import java.util.*

data class Option(var text : String,
                  val showCheckbox : Boolean?,
                  var isChecked : Boolean = false,
                  val id : String = UUID.randomUUID().toString())

class OptionsListAdapter : ListAdapter<Option, OptionViewHolder>(DIFFUTIL_CALLBACK) {
    lateinit var refreshSubmitButton :() -> Unit

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<Option>() {
            override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean =
                oldItem.id == newItem.id


            override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean =
                oldItem == newItem

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = LayoutPollQuizOptionsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding,::getItem, ::selectOnlyCurrentOption, refreshSubmitButton)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.binding.setTheme()
        // Put the cursor in the edittext as it's created.
        holder.binding.text.requestFocus()
        holder.binding.text.hint = "Option ${position+1}"
        holder.binding.text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                getItem(holder.bindingAdapterPosition).text = text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                refreshSubmitButton()
            }

        })
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Option>,
        currentList: MutableList<Option>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        refreshSubmitButton()
    }

    private fun selectOnlyCurrentOption(position: Int) {
        for ( i in 0 until currentList.size) {
            getItem(i).isChecked = i == position
            if(i != position)
                notifyItemChanged(i)
        }
    }
}