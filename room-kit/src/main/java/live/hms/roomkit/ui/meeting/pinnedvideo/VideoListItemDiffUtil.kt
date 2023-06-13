package live.hms.roomkit.ui.meeting.pinnedvideo

import androidx.recyclerview.widget.DiffUtil
import live.hms.roomkit.ui.meeting.audiomode.AudioItemsDiffUtil

class VideoListItemDiffUtil(
  private val oldList: List<VideoListItem>,
  private val newList: List<VideoListItem>
) : DiffUtil.Callback() {

  enum class PayloadKey {
    VALUE
  }

  override fun getOldListSize() = oldList.size

  override fun getNewListSize() = newList.size

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition].track.peer.peerID == newList[newItemPosition].track.peer.peerID
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition].track == newList[newItemPosition].track
            && oldList[oldItemPosition].isTrackMute == newList[newItemPosition].isTrackMute
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    if (oldItemPosition >= oldList.size || newItemPosition >= newList.size) {
      return null
    }

    val old = oldList[oldItemPosition]
    val new = newList[newItemPosition]

    if (old.isTrackMute != new.isTrackMute) {
      return VideoListAdapter.PeerUpdatePayloads.SpeakerMuteUnmute(new.isTrackMute)
    }

    return listOf(PayloadKey.VALUE)
  }
}