package live.hms.android100ms.ui.meeting.pinnedvideo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import live.hms.android100ms.databinding.ListItemVideoBinding
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.NameUtils
import live.hms.android100ms.util.crashlyticsLog

class VideoListAdapter(
  private val onVideoItemClick: (item: MeetingTrack) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoItemViewHolder>() {

  companion object {
    private const val TAG = "VideoListAdapter"
  }

  inner class VideoItemViewHolder(
    val binding: ListItemVideoBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    var bindedItem: MeetingTrack? = null

    fun bind(item: VideoListItem) {
      binding.nameInitials.text = NameUtils.getInitials(item.track.peer.userName)
      binding.name.text = item.track.peer.userName

      binding.root.setOnClickListener { onVideoItemClick(item.track) }

      // TODO: Release context when not viewed somehow !
      /* binding.surfaceView.apply {
        var alreadyBinded = false

        bindedItem?.let {
          alreadyBinded = true
          SurfaceViewRendererUtil.unbind(this, it)
          visibility = View.GONE
        }

        if (!alreadyBinded) {
          setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
          setEnableHardwareScaler(true)
        }

        SurfaceViewRendererUtil.bind(this, item.track).let { success ->
          if (success) visibility = View.VISIBLE
        }
        bindedItem = item.track
      } */
    }
  }

  private val items = ArrayList<VideoListItem>()

  /**
   * @param newItems: Complete list of video items which needs
   *  to be updated in the VideoGrid
   */
  @MainThread
  fun setItems(newItems: MutableList<MeetingTrack>) {
    val newVideoItems = newItems.mapIndexed { index, track -> VideoListItem(index.toLong(), track) }

    val callback = VideoListItemDiffUtil(items, newVideoItems)
    val diff = DiffUtil.calculateDiff(callback)
    items.clear()
    items.addAll(newVideoItems)
    diff.dispatchUpdatesTo(this)

    crashlyticsLog(TAG, "Updated video list: size=${items.size}")
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
    val binding = ListItemVideoBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )

    crashlyticsLog(TAG, "onCreateViewHolder(viewType=$viewType)")
    return VideoItemViewHolder(binding)
  }

  override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
    crashlyticsLog(TAG, "onBindViewHolder: ${items[position]}")
    holder.bind(items[position])
  }


  override fun getItemCount() = items.size
}