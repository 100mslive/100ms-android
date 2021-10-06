package live.hms.app2.ui.meeting.participants

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import live.hms.app2.databinding.LayoutRtmpUrlItemBinding

class RtmpRecordAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<String, RtmpViewHolder>(DIFFUTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtmpViewHolder {
        val binding = LayoutRtmpUrlItemBinding.inflate(LayoutInflater.from(parent.context))
        return RtmpViewHolder(binding, ::getItem, onItemClick)
    }

    override fun onBindViewHolder(holder: RtmpViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    companion object {
        private val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem


            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem


        }
    }
}