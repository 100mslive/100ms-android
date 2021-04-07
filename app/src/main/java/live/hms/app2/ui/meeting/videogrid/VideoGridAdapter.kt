package live.hms.app2.ui.meeting.videogrid

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

class VideoGridAdapter(parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {

  companion object {
    const val TAG = "VideoGridAdapter"
  }

  var totalPages = 0
    set(value) {
      val callback = VideoGridPagerDiffUtil(field, value)
      val diff = DiffUtil.calculateDiff(callback)
      field = value
      diff.dispatchUpdatesTo(this)
    }

  override fun getItemCount() = totalPages
  override fun createFragment(position: Int) = VideoGridPageFragment.newInstance(position)
  override fun getItemId(position: Int) = position.toLong()
  override fun containsItem(itemId: Long) = itemId < totalPages
}