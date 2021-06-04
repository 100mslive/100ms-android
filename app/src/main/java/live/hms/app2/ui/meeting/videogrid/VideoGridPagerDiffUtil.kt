package live.hms.app2.ui.meeting.videogrid

import androidx.recyclerview.widget.DiffUtil

class VideoGridPagerDiffUtil(
  private val oldPageCount: Int,
  private val newPageCount: Int
) : DiffUtil.Callback() {

  override fun getOldListSize() = oldPageCount
  override fun getNewListSize() = newPageCount

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return (oldItemPosition < oldPageCount) && (newItemPosition < newPageCount) && (oldItemPosition == newItemPosition)
  }

  /** We rely on the observers in [VideoGridPageFragment] to update the content.
   * Hence, even if the content is not same, we know that the observers will
   * handle the changes in tracks */
  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return true
  }
}