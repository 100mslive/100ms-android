package live.hms.app2.ui.meeting.participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemPeerListBinding
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.role.HMSRole

class ParticipantsAdapter(
  val isAllowedToChangeRole: Boolean,
  private val availableRoles: List<HMSRole>,
  private val showSheet : (HMSPeer) -> Unit
) : RecyclerView.Adapter<ParticipantsAdapter.PeerViewHolder>() {

  companion object {
    private const val TAG = "ParticipantsAdapter"
  }

  private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

  inner class PeerViewHolder(
    private val binding: ListItemPeerListBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    init {
      with(binding) {
        roleChange.setOnClickListener { showSheet(items[adapterPosition]) }
      }
    }

    fun bind(item: HMSPeer) {
      with(binding) {
        name.text = item.name
        iconScreenShare.visibility = v(item.auxiliaryTracks.isNotEmpty())
        iconAudioOff.visibility = v(item.audioTrack?.isMute != false)
        iconVideoOff.visibility = v(item.videoTrack?.isMute != false)
        roleChange.text = item.hmsRole.name
        // Show change role option only if the role of the local peer allows
        //  and if it's not the local peer itself.
        roleChange.isEnabled = isAllowedToChangeRole && !item.isLocal
      }
    }

  }

  private val items = ArrayList<HMSPeer>()

  @MainThread
  fun setItems(newItems: Array<HMSPeer>) {
    items.clear()
    items.addAll(newItems)
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
    val binding = ListItemPeerListBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return PeerViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size
}