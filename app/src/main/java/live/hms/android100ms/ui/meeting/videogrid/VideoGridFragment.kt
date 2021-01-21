package live.hms.android100ms.ui.meeting.videogrid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils
import live.hms.android100ms.databinding.FragmentVideoGridBinding
import live.hms.android100ms.databinding.GridItemVideoBinding
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.util.viewLifecycle
import org.webrtc.RendererCommon
import kotlin.math.max
import kotlin.math.min

class VideoGridFragment(
  private val initialVideos: MutableList<MeetingTrack>,

  // TODO: Add assert check for maxRows and maxColumns values
  private val maxRows: Int, private val maxColumns: Int,

  private val onVideoItemClick: (video: MeetingTrack) -> Unit
) : Fragment() {

  companion object {
    const val TAG = "VideoGridFragment"
  }

  private var binding by viewLifecycle<FragmentVideoGridBinding>()

  private data class RenderedViewPair(
    val binding: GridItemVideoBinding,
    val video: MeetingTrack
  )

  private val renderedViews = ArrayList<RenderedViewPair>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentVideoGridBinding.inflate(inflater, container, false)
    initGridLayout()
    return binding.root
  }

  private val rows: Int
    get() = min(max(1, renderedViews.size), maxRows)

  private val columns: Int
    get() {
      val result = max(1, (renderedViews.size + rows - 1) / rows)
      if (result > maxColumns) {
        val videos = renderedViews.map { it.video }
        throw IllegalStateException(
          "At most ${maxRows * maxColumns} videos are allowed. Provided $videos"
        )
      }

      return result
    }

  private fun updateGridLayoutDimensions() {
    var colIdx = 0
    var rowIdx = 0

    binding.container.apply {
      for (child in children) {
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

      // Forces maxIndex to be recalculated when columnCount is set
      requestLayout()

      rowCount = rows
      columnCount = columns
    }

    Log.v(TAG, "updateGridLayoutDimensions: ${rows}x${columns}")
  }

  private fun initGridLayout() {
    updateGridLayoutDimensions()

    binding.container.apply {
      for (video in initialVideos) {
        val videoBinding = createVideoView(this)
        bindVideo(videoBinding, video)
        addView(videoBinding.root)
        renderedViews.add(RenderedViewPair(videoBinding, video))
      }
    }

    Log.v(TAG, "Initialized GridLayout with ${initialVideos.size} views")

    updateGridLayoutDimensions()
  }

  fun updateVideos(newVideos: List<MeetingTrack>) {
    val newRenderedViews = ArrayList<RenderedViewPair>()

    // Remove all the views which are not required now
    for (currentRenderedView in renderedViews) {
      val newVideo = newVideos.find { it == currentRenderedView.video }
      if (newVideo == null) {
        binding.container.apply {
          unbindVideo(currentRenderedView.binding, currentRenderedView.video)

          Log.v(TAG, "updateVideos: Removing video=${currentRenderedView.video} from fragment=$tag")
          removeView(currentRenderedView.binding.root)
        }
      }
    }

    for (newVideo in newVideos) {
      // Check if video already rendered
      val renderedViewPair = renderedViews.find { it.video == newVideo }
      if (renderedViewPair != null) {

        Log.v(TAG, "updateVideos: Keeping video=$newVideo in fragment=$tag")
        newRenderedViews.add(renderedViewPair)
      } else {
        // Create a new view
        val videoBinding = createVideoView(binding.container)
        bindVideo(videoBinding, newVideo)
        Log.v(TAG, "updateVideos: Removing video=${newVideo} from fragment=$tag")
        binding.container.addView(videoBinding.root)
        newRenderedViews.add(RenderedViewPair(videoBinding, newVideo))
      }
    }


    Log.v(
      TAG,
      "updateVideos: Change grid items from ${renderedViews.size} -> ${newRenderedViews.size}"
    )

    renderedViews.clear()
    renderedViews.addAll(newRenderedViews)

    updateGridLayoutDimensions()
  }

  private fun bindVideo(binding: GridItemVideoBinding, item: MeetingTrack) {
    binding.container.setOnClickListener { onVideoItemClick(item) }

    binding.name.text = item.peer.userName

    binding.nameInitials.text = item.peer.userName.let { value ->
      if (value.isEmpty()) {
        "--"
      } else {
        value.split(' ')
          .mapNotNull { it.firstOrNull()?.toString() }
          .reduce { acc, s -> acc + s }
      }
    }

    val events = object : RendererCommon.RendererEvents {
      override fun onFirstFrameRendered() {
        Log.v(TAG, "$item SurfaceViewRendered.onFirstFrameRendered()")
      }

      override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
        Log.v(TAG, "$item SurfaceViewRendered.onFrameResolutionChanged($p0, $p1, $p2)")
      }
    }

    // TODO: Add listener for video stream on/off -> Change visibility of surface renderer

    val isVideoAvailable = item.videoTrack != null

    binding.nameInitials.visibility = if (isVideoAvailable) View.GONE else View.VISIBLE
    binding.surfaceView.visibility = if (isVideoAvailable) View.VISIBLE else View.GONE

    if (isVideoAvailable) binding.surfaceView.apply {
      init(HMSWebRTCEglUtils.getRootEglBaseContext(), events)
      setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
      setEnableHardwareScaler(true)
      item.videoTrack?.addSink(this)
    }
  }

  private fun unbindVideo(binding: GridItemVideoBinding, item: MeetingTrack) {
    binding.surfaceView.apply {
      item.videoTrack?.removeSink(this)
      release()
      clearImage()
    }
  }

  private fun createVideoView(parent: ViewGroup): GridItemVideoBinding {
    return GridItemVideoBinding.inflate(
      LayoutInflater.from(context),
      parent,
      false
    )
  }

}