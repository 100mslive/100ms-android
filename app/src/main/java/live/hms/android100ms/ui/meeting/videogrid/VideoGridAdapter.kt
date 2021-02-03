package live.hms.android100ms.ui.meeting.videogrid

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.ThreadUtils
import live.hms.android100ms.util.crashlyticsLog
import kotlin.math.min

class VideoGridAdapter(
  private val parentFragment: Fragment,
  private val onVideoItemClick: (video: MeetingTrack) -> Unit
) : FragmentStateAdapter(parentFragment) {

  companion object {
    const val TAG = "VideoGridAdapter"

    private const val DEBOUNCED_UPDATE_DELAY = 500L
  }

  private val items = ArrayList<MeetingTrack>()
  private val itemsPendingUpdate = ArrayList<MeetingTrack>()

  private val pageItems = ArrayList<VideoGridPageItem>()

  private val context = parentFragment.requireContext()
  private val settings = SettingsStore(context)

  private val itemsPerPage = settings.videoGridRows * settings.videoGridColumns

  private val setItemsHandler = Handler(Looper.getMainLooper())

  /** Used by [getPageForPosition] to assign new id's */
  private var pageItemCurrId: Long = 0

  private fun getVideosForPosition(position: Int): Array<MeetingTrack> {
    val pageVideos = ArrayList<MeetingTrack>()

    // Range is [fromIndex, toIndex] -- Notice the bounds
    val fromIndex = position * itemsPerPage
    val toIndex = min(items.size, (position + 1) * itemsPerPage) - 1

    for (idx in fromIndex..toIndex step 1) {
      pageVideos.add(items[idx])
    }

    return pageVideos.toTypedArray()
  }

  /**
   * Creates a new array list and add references to the MeetingTracks
   * from the parent list [items]
   *
   * @param position: ViewPager page index (starts from 0)
   * @return list of MeetingTrack which needs to be shown in a page
   *  wrapped in [VideoGridPageItem]
   *
   * NOTE: [VideoGridPageItem] instance always have a new id
   *  assigned to it.
   */
  private fun getPageForPosition(position: Int): VideoGridPageItem {
    pageItemCurrId += 1
    return VideoGridPageItem(pageItemCurrId, getVideosForPosition(position))
  }

  private val setItemsRunnable = Runnable {
    ThreadUtils.checkIsOnMainThread()

    // Update the private list [items] used to create pages
    items.clear()
    items.addAll(itemsPendingUpdate)
    itemsPendingUpdate.clear()


    // Keep as many pageItems possible
    // with the same id. Hence, we update the list of pageItem
    // one by one. When we run out of pageItem we create new.

    val newPageItems = ArrayList<VideoGridPageItem>()

    var itemIdx = 0
    var pageIdx = 0

    while (itemIdx < items.size) {
      if (pageIdx < pageItems.size) {
        // Create a new page with same id
        val page = pageItems[pageIdx]
        val videos = getVideosForPosition(pageIdx)
        val newPage = VideoGridPageItem(page.id, videos)
        newPageItems.add(newPage)
        crashlyticsLog(TAG, "Created $newPage replacing $page")
      } else {
        // Create a brand new page
        val page = getPageForPosition(pageIdx)
        newPageItems.add(page)
        crashlyticsLog(TAG, "Created new $page")
      }

      pageIdx += 1
      itemIdx += newPageItems.last().items.size
    }


    val callback = VideoGridPagerDiffUtil(pageItems, newPageItems)
    val diff = DiffUtil.calculateDiff(callback)
    pageItems.clear()
    pageItems.addAll(newPageItems)
    diff.dispatchUpdatesTo(this)

    crashlyticsLog(TAG, "Updated pageItems: size=${pageItems.size}")
  }

  /**
   * This method a debounced-delay of [DEBOUNCED_UPDATE_DELAY] such that
   * if it called multiple times with delay less than debounce,
   * it will update the view just once.
   *
   * @param newItems: Complete list of video items which needs
   *  to be updated in the VideoGrid
   */
  fun setItems(newItems: MutableList<MeetingTrack>) {
    itemsPendingUpdate.clear()
    itemsPendingUpdate.addAll(newItems)

    setItemsHandler.apply {
      removeCallbacks(setItemsRunnable)
      postDelayed(setItemsRunnable, DEBOUNCED_UPDATE_DELAY)
    }
  }

  fun clearItems() {
    itemsPendingUpdate.clear()
    setItemsHandler.apply {
      removeCallbacks(setItemsRunnable)
      postDelayed(setItemsRunnable, DEBOUNCED_UPDATE_DELAY)
    }
  }

  // TODO: Listen to changes in rows, columns in settings
  override fun getItemCount() = pageItems.size

  override fun createFragment(position: Int): Fragment {
    val page = pageItems[position]
    val rows = settings.videoGridRows
    val columns = settings.videoGridColumns

    crashlyticsLog(TAG, "createFragment($position): videos=${page.items}, size=${rows}x${columns}")

    return VideoGridPageFragment(page.items, rows, columns, onVideoItemClick)
  }

  override fun getItemId(position: Int): Long {
    return pageItems[position].id
  }

  override fun containsItem(itemId: Long): Boolean {
    return pageItems.any { it.id == itemId }
  }

  override fun onBindViewHolder(
    holder: FragmentViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {

    if (payloads.isEmpty()) {
      return super.onBindViewHolder(holder, position, payloads)
    }

    // Else manually update the fragment

    val tag = "f${holder.itemId}"
    val fragment = parentFragment
      .childFragmentManager
      .findFragmentByTag(tag)

    crashlyticsLog(
      TAG,
      "onBindViewHolder($holder, $position, $payloads): "
          + "fragment-tag=$tag, "
          + "pageCount=$itemCount, itemsPerPage=$itemsPerPage"
    )

    if (fragment != null) {
      // Manually update the fragment
      val page = pageItems[position]
      crashlyticsLog(
        TAG,
        "onBindViewHolder: Manually updating fragment-tag=$tag with " +
            "total ${page.items.size} [$page]"
      )
      (fragment as VideoGridPageFragment).updateVideos(page.items)

    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }
}