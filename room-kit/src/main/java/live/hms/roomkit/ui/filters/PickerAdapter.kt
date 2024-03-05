package live.hms.roomkit.ui.filters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.R


class PickerAdapter(
    private val context: Context,
    private var dataList: List<String>,
    private val recyclerView: RecyclerView?
) : RecyclerView.Adapter<PickerAdapter.TextVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextVH {
        val view: View
        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(R.layout.layout_picker, parent, false)
        return TextVH(view)
    }

    override fun onBindViewHolder(holder: TextVH, position: Int) {
        holder.pickerTxt.text = dataList[position]
        holder.pickerTxt.setOnClickListener { recyclerView?.smoothScrollToPosition(position) }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun swapData(newData: List<String>) {
        dataList = newData
        notifyDataSetChanged()
    }

    class TextVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pickerTxt: TextView

        init {
            pickerTxt = itemView.findViewById<View>(R.id.picker_item) as TextView
        }
    }
}