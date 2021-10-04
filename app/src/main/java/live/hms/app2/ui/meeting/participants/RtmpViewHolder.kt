package live.hms.app2.ui.meeting.participants

import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.LayoutRtmpUrlItemBinding

class RtmpViewHolder(
    val binding: LayoutRtmpUrlItemBinding,
    getItem: (Int) -> String,
    clicked: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.removeButton.setOnClickListener { clicked(getItem(adapterPosition)) }
    }

    fun onBind(string: String) {
        binding.rtmpUrl.text = string
    }
}