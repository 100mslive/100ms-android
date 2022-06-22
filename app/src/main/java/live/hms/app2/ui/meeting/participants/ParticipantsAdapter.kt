package live.hms.app2.ui.meeting.participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemPeerListBinding
import live.hms.video.sdk.models.HMSPeer

enum class VIEW_TYPE {
  PREVIEW,MEETING
}
class ParticipantsAdapter(
  val isAllowedToChangeRole: Boolean,
  val isAllowedToKickPeer : Boolean,
  val isAllowedToMutePeer : Boolean,
  val isAllowedToAskUnmutePeer : Boolean,
  private val showSheet : (HMSPeer) -> Unit,
  val type: VIEW_TYPE = VIEW_TYPE.MEETING,
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
        peerSettings.setOnClickListener { showSheet(items[adapterPosition]) }
      }
    }

    fun bind(item: HMSPeer) {
      with(binding) {
        name.text = item.name
        iconScreenShare.visibility = v(item.auxiliaryTracks.isNotEmpty())
        if (VIEW_TYPE.PREVIEW == type){
          iconAudioOff.visibility = View.GONE
          iconVideoOff.visibility = View.GONE
        } else {
          iconAudioOff.visibility = v(item.audioTrack?.isMute != false)
          iconVideoOff.visibility = v(item.videoTrack?.isMute != false)
        }

        peerRole.text = item.hmsRole.name
        // Show change role option only if the role of the local peer allows
        //  and if it's not the local peer itself.
        peerSettings.visibility = v(isAllowedToChangeRole || isAllowedToAskUnmutePeer || isAllowedToMutePeer || isAllowedToKickPeer )
      }
    }

  }

  private val items = ArrayList<HMSPeer>()

  @MainThread
  fun setItems(newItems: List<HMSPeer>) {
    items.clear()
    items.addAll(newItems)
    notifyDataSetChanged()
  }

  @MainThread
  fun getItems() = items

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
    val binding = ListItemPeerListBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return PeerViewHolder(binding)
  }

  fun removeItem(item : HMSPeer?){
    items.remove(item)
    notifyDataSetChanged()
  }

  fun insertItem(item : HMSPeer) {
    items.add(item)
    notifyItemInserted(items.size - 1)
  }

  override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size
}