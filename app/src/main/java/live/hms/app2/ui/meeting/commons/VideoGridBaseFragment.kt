package live.hms.app2.ui.meeting.commons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.databinding.GridItemVideoBinding
import live.hms.app2.databinding.VideoCardBinding
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.NameUtils
import live.hms.app2.util.SurfaceViewRendererUtil
import live.hms.app2.util.crashlyticsLog
import live.hms.app2.util.visibility
import live.hms.video.sdk.models.HMSSpeaker
import org.webrtc.RendererCommon
import kotlin.math.max
import kotlin.math.min

/**
 * The Grid is created by building column by column.
 * Example: For 4x2 (rows x columns)
 *  - 3 videos will have 3 rows, 1 column
 *  - 5 videos will have 4 rows, 2 columns
 *  - 8 videos will have 4 rows, 2 columns
 */
abstract class VideoGridBaseFragment : Fragment() {
  companion object {
    private const val TAG = "VideoGridBase"
  }

  protected val settings: SettingsStore by lazy { SettingsStore(requireContext()) }
  protected val meetingViewModel by activityViewModels<MeetingViewModel>()

  // Determined using the onResume() and onPause()
  var isViewVisible = false
    private set

  protected data class RenderedViewPair(
    val binding: GridItemVideoBinding,
    val meetingTrack: MeetingTrack
  )

  private val bindedVideoTrackIds = mutableSetOf<String>()
  protected val renderedViews = ArrayList<RenderedViewPair>()

  protected val rows: Int
    get() = min(max(1, renderedViews.size), settings.videoGridRows)

  protected val columns: Int
    get() {
      val maxColumns = settings.videoGridColumns
      val result = max(1, (renderedViews.size + rows - 1) / rows)
      if (result > maxColumns) {
        val videos = renderedViews.map { it.meetingTrack }
        throw IllegalStateException(
          "At most ${settings.videoGridRows * maxColumns} videos are allowed. Provided $videos"
        )
      }

      return result
    }

  protected val maxItems: Int
    get() = settings.videoGridRows * settings.videoGridColumns

  private fun updateGridLayoutDimensions(layout: GridLayout) {
    crashlyticsLog(TAG, "updateGridLayoutDimensions: ${rows}x${columns}")

    var childIdx: Pair<Int, Int>? = null
    var colIdx = 0
    var rowIdx = 0

    layout.apply {
      crashlyticsLog(TAG, "Updating GridLayout.spec for ${children.count()} children")
      for (child in children) {
        childIdx = Pair(rowIdx, colIdx)

        val params = child.layoutParams as GridLayout.LayoutParams
        params.rowSpec = GridLayout.spec(rowIdx, 1, 1f)
        params.columnSpec = GridLayout.spec(colIdx, 1, 1f)

        if (colIdx + 1 == columns) {
          rowIdx += 1
          colIdx = 0
        } else {
          colIdx += 1
        }
      }

      crashlyticsLog(TAG, "Changed GridLayout's children spec with bottom-right at $childIdx")

      // Forces maxIndex to be recalculated when rowCount/columnCount is set
      requestLayout()

      rowCount = rows
      columnCount = columns
    }
  }

  private fun createVideoView(parent: ViewGroup): GridItemVideoBinding {
    return GridItemVideoBinding.inflate(
      LayoutInflater.from(requireContext()),
      parent,
      false
    )
  }

  protected fun bindSurfaceView(
    binding: VideoCardBinding,
    item: MeetingTrack,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED
  ) {
    if (item.video == null || item.video?.isMute == true) return

    binding.surfaceView.let { view ->
      view.setScalingType(scalingType)
      view.setEnableHardwareScaler(true)

      SurfaceViewRendererUtil.bind(view, item).let {
        if (it) {
          binding.surfaceView.visibility = View.VISIBLE
          bindedVideoTrackIds.add(item.video!!.trackId)
        }
      }
    }
  }

  protected fun bindVideo(binding: VideoCardBinding, item: MeetingTrack) {
    // FIXME: Add a shared VM with activity scope to subscribe to events
    // binding.container.setOnClickListener { viewModel.onVideoItemClick?.invoke(item) }

    binding.apply {
      name.text = item.peer.name
      nameInitials.text = NameUtils.getInitials(item.peer.name)
      iconScreenShare.visibility = if (item.isScreen) View.VISIBLE else View.GONE
      iconAudioOff.visibility = visibility(
        item.isScreen.not() &&
            (item.audio == null || item.audio!!.isMute)
      )
      icDegraded.visibility = if(item.video?.isDegraded == true) View.VISIBLE else View.GONE

      /** [View.setVisibility] */
      val surfaceViewVisibility = if (item.video == null || item.video?.isMute == true) {
        View.INVISIBLE
      } else {
        View.VISIBLE
      }

      if (surfaceView.visibility != surfaceViewVisibility) {
        surfaceView.visibility = surfaceViewVisibility
      }
    }
  }

  protected fun unbindSurfaceView(
    binding: VideoCardBinding,
    item: MeetingTrack,
    metadata: String = ""
  ) {
    if (!bindedVideoTrackIds.contains(item.video?.trackId ?: "")) return

    SurfaceViewRendererUtil.unbind(binding.surfaceView, item, metadata).let {
      if (it) {
        binding.surfaceView.visibility = View.INVISIBLE
        bindedVideoTrackIds.remove(item.video!!.trackId)
      }
    }
  }


  protected fun updateVideos(layout: GridLayout, newVideos: Array<MeetingTrack?>) {
    crashlyticsLog(
      TAG,
      "updateVideos(${newVideos.size}) -- presently ${renderedViews.size} items in grid"
    )

    var requiresGridLayoutUpdate = false
    val newRenderedViews = ArrayList<RenderedViewPair>()

    // Remove all the views which are not required now
    for (currentRenderedView in renderedViews) {
      val newVideo = newVideos.find { it == currentRenderedView.meetingTrack }
      if (newVideo == null) {
        crashlyticsLog(
          TAG,
          "updateVideos: Removing view for video=${currentRenderedView.meetingTrack} in fragment=$tag"
        )
        requiresGridLayoutUpdate = true

        layout.apply {
          // Unbind only when view is visible to user
          if (isViewVisible) unbindSurfaceView(
            currentRenderedView.binding.videoCard,
            currentRenderedView.meetingTrack
          )
          removeViewInLayout(currentRenderedView.binding.root)
        }
      }
    }

    for (_newVideo in newVideos) {
      _newVideo?.also { newVideo ->

        // Check if video already rendered
        val renderedViewPair = renderedViews.find { it.meetingTrack == newVideo }
        if (renderedViewPair != null) {
          crashlyticsLog(TAG, "updateVideos: Keeping view for video=$newVideo  in fragment=$tag")
          newRenderedViews.add(renderedViewPair)

          if (isViewVisible && !bindedVideoTrackIds.contains(newVideo.video?.trackId ?: "")) {
            // This view is not yet initialized (possibly because when AudioTrack was added --
            // VideoTrack was not present, hence had to create an empty tile)
            bindSurfaceView(renderedViewPair.binding.videoCard, newVideo)
          }

        } else {
          crashlyticsLog(TAG, "updateVideos: Creating view for video=${newVideo} in fragment=$tag")
          requiresGridLayoutUpdate = true

          // Create a new view
          val videoBinding = createVideoView(layout)

          // Bind surfaceView when view is visible to user
          if (isViewVisible) {
            bindSurfaceView(videoBinding.videoCard, newVideo)
          }

          layout.addView(videoBinding.root)
          newRenderedViews.add(RenderedViewPair(videoBinding, newVideo))
        }
      }
    }

    crashlyticsLog(
      TAG,
      "updateVideos: Change grid items from ${renderedViews.size} -> ${newRenderedViews.size}"
    )


    renderedViews.clear()
    renderedViews.addAll(newRenderedViews)

    // Re-bind all the videos, this handles any changes made in isMute
    for (view in renderedViews) {
      bindVideo(view.binding.videoCard, view.meetingTrack)
    }

    if (requiresGridLayoutUpdate) {
      updateGridLayoutDimensions(layout)
    }
  }

  protected fun applySpeakerUpdates(speakers: Array<HMSSpeaker>) {
    renderedViews.forEach { renderedView ->
      val track = renderedView.meetingTrack.audio
      renderedView.binding.apply {
        if (track == null || track.isMute) {
          videoCard.audioLevel.apply {
            text = "-"
          }
          container.strokeWidth = 0
        } else {
          val level = speakers.find { it.trackId == track.trackId }?.level ?: 0

          videoCard.audioLevel.apply {
            text = "$level"
          }

          when {
            level >= 70 -> {
              container.strokeWidth = 6
            }
            70 > level && level >= settings.silenceAudioLevelThreshold -> {
              container.strokeWidth = 4
            }
            else -> {
              container.strokeWidth = 0
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    crashlyticsLog(TAG, "Fragment=$tag onResume()")
    isViewVisible = true

    renderedViews.forEach {
      bindSurfaceView(it.binding.videoCard, it.meetingTrack)
    }
  }

  override fun onPause() {
    super.onPause()
    crashlyticsLog(TAG, "Fragment=$tag onPause()")
    isViewVisible = false

    renderedViews.forEach {
      unbindSurfaceView(it.binding.videoCard, it.meetingTrack)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    crashlyticsLog(TAG, "Fragment=$tag onDestroy()")

    // Release all references to views
    renderedViews.clear()
  }
}