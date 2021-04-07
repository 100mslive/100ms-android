package live.hms.app2.ui.meeting.videogrid

import androidx.recyclerview.widget.DiffUtil

class VideoGridPagerDiffUtil(
    private val oldPageCount: Int,
    private val newPageCount: Int
) : DiffUtil.Callback() {

  enum class PayloadKey { VALUE }

  override fun getOldListSize() = oldPageCount
  override fun getNewListSize() = newPageCount

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return (oldItemPosition < oldPageCount) && (newItemPosition < newPageCount) && (oldItemPosition == newItemPosition)
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return (oldItemPosition < oldPageCount) && (newItemPosition < newPageCount) && (oldItemPosition == newItemPosition)
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
    return listOf(PayloadKey.VALUE)
  }
}