package live.hms.android100ms.ui.meeting.videogrid

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.SettingsStore
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

  public fun setItems(newItems: List<MeetingTrack>) {
    items.clear()
    items.addAll(newItems)

    Log.v(TAG, "Updated items: size=${items.size}")
    notifyDataSetChanged()
  }

  // TODO: Listen to changes in rows, columns in settings
  override fun getItemCount(): Int {
    return (items.size + itemsPerPage - 1) / itemsPerPage
  }

  private fun getVideosForPage(position: Int): MutableList<MeetingTrack> {
    return items.subList(
      position * itemsPerPage,
      min(items.size, (position + 1) * itemsPerPage)
    ) // Range is [fromIndex, toIndex) -- Notice the bounds

  }

  override fun createFragment(position: Int): Fragment {
    val pageVideos = getVideosForPage(position)
    val rows = settings.videoGridRows
    val columns = settings.videoGridColumns

    Log.v(TAG, "createFragment($position): videos=${pageVideos}, size=${rows}x${columns}")

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

    Log.v(
      TAG,
      "onBindViewHolder($holder, $position, $payloads): "
          + "tag=$tag, fragment=$fragment, "
          + "pageCount=$itemCount, itemsPerPage=$itemsPerPage"
    )

    if (fragment != null) {
      // Manually update the fragment
      val newVideos = getVideosForPage(position)
      Log.v(TAG, "onBindViewHolder: Manually updating fragment $tag with newVideos=$newVideos")
      (fragment as VideoGridFragment).updateVideos(newVideos)

    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }
}