package live.hms.roomkit.ui.polls.display

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.roomkit.databinding.LayoutPollsDisplayOptionsItemBinding
import java.util.*

data class Option(var text : String,
                  val showCheckbox : Boolean,
                  var isChecked : Boolean = false, val id : String = UUID.randomUUID().toString(),
var hiddenAndAnswered : Boolean = false)

/**
 * Displays options on the single/multi choice questions.
 * When answers are selected, it sends them to the server.
 * When they're sent, it displays the result for that question.
 *
 * Functions needed are:
 * answer selected, which takes the question and such
 */
class AnswerOptionsAdapter(private val canRoleViewVotes : Boolean,
    private val questionAnswered : () -> Unit) : ListAdapter<Option, DisplayAnswerOptionsViewHolder>(DIFFUTIL_CALLBACK) {

    // all items have in fact changed.
    @SuppressLint("NotifyDataSetChanged")
    fun disableOptions() {
        for (i in 0 until itemCount) {
            val item = getItem(i)
            item.hiddenAndAnswered = true
        }
        notifyDataSetChanged()
    }
    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<Option>() {
            override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean =
                oldItem.id == newItem.id


            override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean =
                oldItem == newItem

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayAnswerOptionsViewHolder {
        val binding = LayoutPollsDisplayOptionsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DisplayAnswerOptionsViewHolder(binding,canRoleViewVotes, ::getItem, ::setItemSelected, questionAnswered)
    }

    override fun onBindViewHolder(holder: DisplayAnswerOptionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // All items are in fact changed
    @SuppressLint("NotifyDataSetChanged")
    fun setItemSelected(position : Int, noOthers : Boolean) {
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if(i == position) {
                item.isChecked = true
            }
            else if (noOthers) {
                item.isChecked = false
            }
        }
        notifyDataSetChanged()
    }

    fun getSelectedOptions() : List<Int>{
        val selectedItems = mutableListOf<Int>()
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if(item.isChecked) {
                selectedItems.add(i)
            }
        }
        return selectedItems
    }
}