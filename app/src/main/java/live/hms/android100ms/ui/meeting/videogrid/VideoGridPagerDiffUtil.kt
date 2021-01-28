package live.hms.android100ms.ui.meeting.videogrid

import androidx.recyclerview.widget.DiffUtil

class VideoGridPagerDiffUtil(
  private val oldList: List<VideoGridPageItem>,
  private val newList: List<VideoGridPageItem>
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
    val oldItem = oldList[oldItemPosition]
    val newItem = newList[newItemPosition]

    if (oldItem.items.size != newItem.items.size) {
      return false
    }

    val size = oldItem.items.size
    for (i in 0 until size) {
      if (oldItem.items[i] != newItem.items[i]) {
        return false
      }
    }

    return true
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
    return listOf(PayloadKey.VALUE)
  }
}