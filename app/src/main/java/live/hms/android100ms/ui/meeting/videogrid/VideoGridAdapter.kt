package live.hms.android100ms.ui.meeting.videogrid

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.crashlyticsLog
import kotlin.math.min

class VideoGridAdapter(
  private val parentFragment: Fragment,
  private val onVideoItemClick: (video: MeetingTrack) -> Unit
) : FragmentStateAdapter(parentFragment) {

  companion object {
    const val TAG = "VideoGridAdapter"
  }

  private val items = ArrayList<MeetingTrack>()

  private val context = parentFragment.requireContext()
  private val settings = SettingsStore(context)

  private val itemsPerPage = settings.videoGridRows * settings.videoGridColumns

  fun setItems(newItems: MutableList<MeetingTrack>) {
    items.clear()
    items.addAll(newItems)

    crashlyticsLog(TAG, "Updated items: size=${items.size}")
    notifyDataSetChanged()
  }

  // TODO: Listen to changes in rows, columns in settings
  override fun getItemCount(): Int {
    return (items.size + itemsPerPage - 1) / itemsPerPage
  }

  /**
   * Creates a new array list and add references to the MeetingTracks
   * from the parent list [items]
   *
   * @param position: ViewPager page index (starts from 0)
   * @return list of MeetingTrack which needs to be shown in a page
   */
  private fun getVideosForPage(position: Int): MutableList<MeetingTrack> {
    val pageVideos = ArrayList<MeetingTrack>()

    // Range is [fromIndex, toIndex] -- Notice the bounds
    val fromIndex = position * itemsPerPage
    val toIndex = min(items.size, (position + 1) * itemsPerPage) - 1

    for (idx in fromIndex..toIndex step 1) {
      pageVideos.add(items[idx])
    }

    return pageVideos
  }

  override fun createFragment(position: Int): Fragment {
    val pageVideos = getVideosForPage(position)
    val rows = settings.videoGridRows
    val columns = settings.videoGridColumns

    crashlyticsLog(TAG, "createFragment($position): videos=${pageVideos}, size=${rows}x${columns}")

    return VideoGridFragment(pageVideos, rows, columns, onVideoItemClick)
  }

  override fun onBindViewHolder(
    holder: FragmentViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    val tag = "f${holder.itemId}"
    val fragment = parentFragment
      .childFragmentManager
      .findFragmentByTag(tag)

    crashlyticsLog(
      TAG,
      "onBindViewHolder($holder, $position, $payloads): "
          + "tag=$tag, fragment=$fragment, "
          + "pageCount=$itemCount, itemsPerPage=$itemsPerPage"
    )

    if (fragment != null) {
      // Manually update the fragment
      val newVideos = getVideosForPage(position)
      crashlyticsLog(
        TAG,
        "onBindViewHolder: Manually updating fragment $tag with " +
            "total ${newVideos.size} [newVideos=$newVideos]"
      )
      (fragment as VideoGridFragment).updateVideos(newVideos)

    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }
}