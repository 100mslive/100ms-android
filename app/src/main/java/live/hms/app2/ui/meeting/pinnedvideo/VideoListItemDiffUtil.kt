package live.hms.app2.ui.meeting.pinnedvideo

import androidx.recyclerview.widget.DiffUtil

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
    return oldList[oldItemPosition].id == newList[newItemPosition].id
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return oldList[oldItemPosition].track == newList[newItemPosition].track
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    return listOf(PayloadKey.VALUE)
  }
}