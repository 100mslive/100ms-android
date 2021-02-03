package live.hms.android100ms.ui.meeting.pinnedvideo

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import live.hms.android100ms.databinding.ListItemVideoBinding
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.NameUtils
import live.hms.android100ms.util.ThreadUtils
import live.hms.android100ms.util.crashlyticsLog

class VideoListAdapter(
  private val onVideoItemClick: (item: VideoListItem) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoItemViewHolder>() {

  companion object {
    private const val TAG = "VideoListAdapter"

    private const val DEBOUNCED_UPDATE_DELAY = 500L
  }

  inner class VideoItemViewHolder(
    val binding: ListItemVideoBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: VideoListItem) {
      binding.nameInitials.text = NameUtils.getInitials(item.track.peer.userName)
    }
  }

  private val items = ArrayList<VideoListItem>()
  private val tracksPendingUpdate = ArrayList<MeetingTrack>()

  private val setItemsHandler = Handler(Looper.getMainLooper())

  private val setItemsRunnable = Runnable {
    ThreadUtils.checkIsOnMainThread()

    val newItems = ArrayList<VideoListItem>()
    tracksPendingUpdate.forEachIndexed { index, track ->
      newItems.add(VideoListItem(index.toLong(), track))
    }
    tracksPendingUpdate.clear()

    val callback = VideoListItemDiffUtil(items, newItems)
    val diff = DiffUtil.calculateDiff(callback)
    items.clear()
    items.addAll(newItems)
    diff.dispatchUpdatesTo(this)

    crashlyticsLog(TAG, "Updated video list: size=${items.size}")
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
    tracksPendingUpdate.clear()
    tracksPendingUpdate.addAll(newItems)

    setItemsHandler.apply {
      removeCallbacks(setItemsRunnable)
      postDelayed(setItemsRunnable, DEBOUNCED_UPDATE_DELAY)
    }
  }

  fun clearItems() {
    tracksPendingUpdate.clear()

    setItemsHandler.apply {
      removeCallbacks(setItemsRunnable)
      postDelayed(setItemsRunnable, DEBOUNCED_UPDATE_DELAY)
    }
  }


  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
    val binding = ListItemVideoBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )

    crashlyticsLog(TAG, "onCreateViewHolder($parent, $viewType)")
    return VideoItemViewHolder(binding)
  }

  override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
    crashlyticsLog(TAG, "onBindViewHolder: ${items[position]}")
    holder.bind(items[position])
  }

  override fun getItemCount() = items.size
}