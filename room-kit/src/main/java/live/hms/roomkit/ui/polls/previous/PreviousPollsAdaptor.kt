package live.hms.roomkit.ui.polls.previous

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.databinding.PreviousPollsListBinding
import live.hms.roomkit.ui.theme.pollsStatusLiveDraftEnded
import live.hms.video.polls.models.HmsPollState

data class PreviousPollsInfo(
    val name: String,
    val state: HmsPollState,
    val pollId: String
)

class PreviewPollsViewBinding(
    val binding: PreviousPollsListBinding,
    val view: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(info: PreviousPollsInfo) {
        with(binding) {
            name.text = info.name
            viewButton.isEnabled = info.state == HmsPollState.STARTED
            viewButton.setOnClickListener { view(bindingAdapterPosition) }
            status.pollsStatusLiveDraftEnded(info.state)
        }
    }
}

class PreviousPollsAdaptor(private val view: (PreviousPollsInfo) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<PreviousPollsInfo, PreviewPollsViewBinding>(
        DIFFUTIL_CALLBACK
    ) {

    companion object {
        val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<PreviousPollsInfo>() {
            override fun areItemsTheSame(
                oldItem: PreviousPollsInfo,
                newItem: PreviousPollsInfo
            ) = oldItem.pollId == newItem.pollId

            override fun areContentsTheSame(
                oldItem: PreviousPollsInfo,
                newItem: PreviousPollsInfo
            ) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewPollsViewBinding {
        val binding = PreviousPollsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PreviewPollsViewBinding(binding, {position : Int -> view(getItem(position))})
    }

    override fun onBindViewHolder(holder: PreviewPollsViewBinding, position: Int) {
        holder.bind(getItem(position))
    }
}