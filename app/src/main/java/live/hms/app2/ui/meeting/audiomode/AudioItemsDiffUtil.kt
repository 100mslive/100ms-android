package live.hms.app2.ui.meeting.audiomode

import androidx.recyclerview.widget.DiffUtil

class AudioItemsDiffUtil(
  private val oldItems: Array<AudioItem>,
  private val newItems: Array<AudioItem>,
) : DiffUtil.Callback() {

  enum class PayloadKey { AUDIO_LEVEL, TRACK_STATUS, SAME }

  override fun getOldListSize() = oldItems.size
  override fun getNewListSize() = newItems.size

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    if (oldItemPosition >= oldItems.size || newItemPosition >= newItems.size) {
      return false
    }

    val old = oldItems[oldItemPosition]
    val new = newItems[newItemPosition]
    return old.peerId == new.peerId
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return false
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    if (oldItemPosition >= oldItems.size || newItemPosition >= newItems.size) {
      return null
    }

    val old = oldItems[oldItemPosition]
    val new = newItems[newItemPosition]

    if (old.isTrackMute != new.isTrackMute) {
      return PayloadKey.TRACK_STATUS
    } else if (old.audioLevel != new.audioLevel) {
      return PayloadKey.AUDIO_LEVEL
    }

    return PayloadKey.SAME
  }
}