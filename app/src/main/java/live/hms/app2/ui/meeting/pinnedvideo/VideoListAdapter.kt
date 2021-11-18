package live.hms.app2.ui.meeting.pinnedvideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemVideoBinding
import live.hms.app2.ui.meeting.CustomPeerMetadata
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.util.NameUtils
import live.hms.app2.util.SurfaceViewRendererUtil
import live.hms.app2.util.crashlyticsLog
import live.hms.app2.util.visibility
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import org.webrtc.RendererCommon

class VideoListAdapter(
  private val onVideoItemClick: (item: MeetingTrack) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoItemViewHolder>() {

  companion object {
    private const val TAG = "VideoListAdapter"
  }

  override fun getItemId(position: Int) = items[position].id

  override fun onViewAttachedToWindow(holder: VideoItemViewHolder) {
    super.onViewAttachedToWindow(holder)
    Log.d(TAG, "onViewAttachedToWindow($holder)")
    holder.bindSurfaceView()
  }

  override fun onViewDetachedFromWindow(holder: VideoItemViewHolder) {
    super.onViewDetachedFromWindow(holder)
    Log.d(TAG, "onViewDetachedFromWindow($holder)")
    holder.unbindSurfaceView()
  }

  inner class VideoItemViewHolder(
    val binding: ListItemVideoBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    private var itemRef: VideoListItem? = null

    private var isSurfaceViewBinded = false

    fun bind(item: VideoListItem) {
      binding.nameInitials.text = NameUtils.getInitials(item.track.peer.name)
      binding.name.text = item.track.peer.name
      binding.iconScreenShare.visibility = if (item.track.isScreen) View.VISIBLE else View.GONE

      binding.surfaceView.apply {
        setEnableHardwareScaler(true)
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        // Meanwhile until the video is not binded, hide the view.
        visibility = View.GONE

        // Update the reference such that when view is attached to window
        // surface view is initialized with correct [VideoTrack]
        itemRef = item
        isSurfaceViewBinded = false
      }

      val isHandRaised: Boolean =
        CustomPeerMetadata.fromJson(item.track.peer.metadata)?.isHandRaised == true
      binding.raisedHand.visibility = visibility(isHandRaised)

      binding.root.setOnClickListener { onVideoItemClick(item.track) }
    }


    fun bindSurfaceView() {
      if (isSurfaceViewBinded) {
        Log.d(TAG, "bindSurfaceView: Surface view already initialized")
        return
      }

      itemRef?.let { item ->
        SurfaceViewRendererUtil.bind(
          binding.surfaceView,
          item.track,
          "VideoItemViewHolder::bindSurfaceView"
        ).let { success ->
          if (success) {
            binding.surfaceView.visibility = if (item.track.video?.isDegraded == true) View.INVISIBLE else View.VISIBLE
            isSurfaceViewBinded = true
          }
        }
      }
    }

    fun unbindSurfaceView() {
      if (!isSurfaceViewBinded) return

      itemRef?.let { item ->
        SurfaceViewRendererUtil.unbind(
          binding.surfaceView,
          item.track,
          "VideoItemViewHolder::unbindSurfaceView"
        ).let { success ->
          if (success) {
            binding.surfaceView.visibility = View.GONE
            isSurfaceViewBinded = false
          }
        }
      }
    }
  }

  private val items = ArrayList<VideoListItem>()

  /**
   * @param newItems: Complete list of video items which needs
   *  to be updated in the VideoGrid
   */
  @MainThread
  fun setItems(newItems: List<MeetingTrack>) {
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
    return VideoItemViewHolder(binding)
  }

  override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
    crashlyticsLog(TAG, "onBindViewHolder: ${items[position]}")
    holder.bind(items[position])
  }

  override fun onBindViewHolder(
    holder: VideoItemViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    if (payloads.isEmpty()) {
      return super.onBindViewHolder(holder, position, payloads)
    } else if (payloads.all { it is PeerUpdatePayloads }) {
      // Only if all the payloads are of type peer udpate it makes sense to individually update.
      // If they weren't the non-payload type will result in a full redraw anyway so we let
      // it go to the full redraw in the else clause.
      payloads.forEach {
        if (it is PeerUpdatePayloads) {
          when (it) {
            is PeerUpdatePayloads.MetadataChanged -> {
              holder.binding.raisedHand.visibility =
                visibility(it.metadata?.isHandRaised == true)
            }
            is PeerUpdatePayloads.NameChanged -> {
              holder.binding.nameInitials.text = NameUtils.getInitials(it.name)
              holder.binding.name.text = it.name
            }
          }
        }
      }
    } else {

      crashlyticsLog(
        TAG,
        "onBindViewHolder: Manually updating $holder with ${items[position]} " +
                "[payloads=$payloads]"
      )
      holder.unbindSurfaceView() // Free the context initialized for the previous item
      holder.bind(items[position])
      holder.bindSurfaceView()
    }
  }

  override fun getItemCount() = items.size

  fun itemChanged(changedPeer: Pair<HMSPeer, HMSPeerUpdate>) {

    val updatedItemId = items.find { it.track.peer.peerID == changedPeer.first.peerID }?.id

    val payload = when (changedPeer.second) {
      HMSPeerUpdate.METADATA_CHANGED -> PeerUpdatePayloads.MetadataChanged(
        CustomPeerMetadata.fromJson(
          changedPeer.first.metadata
        )
      )
      HMSPeerUpdate.NAME_CHANGED -> PeerUpdatePayloads.NameChanged(changedPeer.first.name)
      else -> null
    }

    updatedItemId?.toInt()
      ?.let { notifyItemChanged(it, payload) }
  }

  sealed class PeerUpdatePayloads {
    data class NameChanged(val name: String) : PeerUpdatePayloads()
    data class MetadataChanged(val metadata: CustomPeerMetadata?) : PeerUpdatePayloads()
  }

}