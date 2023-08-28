package live.hms.videogrid

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

class VideoGridAdapter(
    private val fragmentManager: FragmentManager,
    private val lifeCycle: Lifecycle,
    private val isScreenShare: Boolean = false
) : FragmentStateAdapter(fragmentManager, lifeCycle) {

    companion object {
        const val TAG = "VideoGridAdapter"
    }

    constructor(
        activity: FragmentActivity, isScreenShare: Boolean = false
    ) : this(activity.supportFragmentManager, activity.lifecycle, isScreenShare)

    constructor(
        parentFragment: Fragment, isScreenShare: Boolean = false
    ) : this(parentFragment.childFragmentManager, parentFragment.lifecycle, isScreenShare)

    var totalPages = 0
        set(value) {
            val callback = VideoGridPagerDiffUtil(field, value)
            val diff = DiffUtil.calculateDiff(callback)
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun getItemCount() = totalPages
    override fun createFragment(position: Int) =
        VideoGridPageFragment.newInstance(position, isScreenShare)

    override fun getItemId(position: Int) = position.toLong()
    override fun containsItem(itemId: Long) = itemId < totalPages
}