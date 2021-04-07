package live.hms.app2.ui.meeting.videogrid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.databinding.FragmentVideoGridPageBinding
import live.hms.app2.databinding.GridItemVideoBinding
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.NameUtils
import live.hms.app2.util.SurfaceViewRendererUtil
import live.hms.app2.util.crashlyticsLog
import live.hms.app2.util.viewLifecycle
import org.webrtc.RendererCommon
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

/**
 * The Grid is created by building column by column.
 * Example: For 4x2 (rows x columns)
 *  - 3 videos will have 3 rows, 1 column
 *  - 5 videos will have 4 rows, 2 columns
 *  - 8 videos will have 4 rows, 2 columns
 */
class VideoGridPageFragment : Fragment() {

  companion object {
    private const val TAG = "VideoGridPageFragment"

    private const val BUNDLE_PAGE_INDEX = "bundle-page-index"

    public fun newInstance(pageIndex: Int): VideoGridPageFragment {
      return VideoGridPageFragment().apply {
        arguments = bundleOf(BUNDLE_PAGE_INDEX to pageIndex)
      }
    }
  }

  private var binding by viewLifecycle<FragmentVideoGridPageBinding>()
  private val meetingViewModel by activityViewModels<MeetingViewModel>()

  // Determined using the onResume() and onPause()
  private var isViewVisible = false

  private data class RenderedViewPair(
    val binding: GridItemVideoBinding,
    val video: MeetingTrack
  )

  private val renderedViews = ArrayList<RenderedViewPair>()

  private var pageIndex by Delegates.notNull<Int>()
  private var maxRows by Delegates.notNull<Int>()
  private var maxColumns by Delegates.notNull<Int>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentVideoGridPageBinding.inflate(inflater, container, false)

    pageIndex = requireArguments()[BUNDLE_PAGE_INDEX] as Int

    // TODO: Listen to changes in rows & columns
    val settings = SettingsStore(requireContext())
    maxRows = settings.videoGridRows
    maxColumns = settings.videoGridColumns

    initViewModels()
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
    crashlyticsLog(TAG, "updateGridLayoutDimensions: ${rows}x${columns}")

    var childIdx: Pair<Int, Int>? = null
    var colIdx = 0
    var rowIdx = 0

    binding.container.apply {
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

  private fun getCurrentPageVideos(tracks: List<MeetingTrack>): Array<MeetingTrack> {
    val pageVideos = ArrayList<MeetingTrack>()

    // Range is [fromIndex, toIndex] -- Notice the bounds
    val itemsCount = maxRows * maxColumns
    val fromIndex = pageIndex * itemsCount
    val toIndex = min(tracks.size, (pageIndex + 1) * itemsCount) - 1

    for (idx in fromIndex..toIndex step 1) {
      pageVideos.add(tracks[idx])
    }

    return pageVideos.toTypedArray()
  }

  private fun initViewModels() {
    meetingViewModel.videoTracks.observe(viewLifecycleOwner) { tracks ->
      val videos = getCurrentPageVideos(tracks)
      updateVideos(videos)
    }
  }

  fun updateVideos(newVideos: Array<MeetingTrack>) {
    crashlyticsLog(
      TAG,
      "updateVideos(${newVideos.size}) -- presently ${renderedViews.size} items in grid"
    )

    val newRenderedViews = ArrayList<RenderedViewPair>()

    // Remove all the views which are not required now
    for (currentRenderedView in renderedViews) {
      val newVideo = newVideos.find { it == currentRenderedView.video }
      if (newVideo == null) {
        crashlyticsLog(
          TAG,
          "updateVideos: Removing view for video=${currentRenderedView.video} from fragment=$tag"
        )

        binding.container.apply {
          // Unbind only when view is visible to user
          if (isViewVisible) unbindSurfaceView(
            currentRenderedView.binding,
            currentRenderedView.video
          )
          removeViewInLayout(currentRenderedView.binding.root)
        }
      }
    }

    for (newVideo in newVideos) {
      // Check if video already rendered
      val renderedViewPair = renderedViews.find { it.video == newVideo }
      if (renderedViewPair != null) {
        crashlyticsLog(TAG, "updateVideos: Keeping view for video=$newVideo in fragment=$tag")
        newRenderedViews.add(renderedViewPair)
      } else {
        crashlyticsLog(TAG, "updateVideos: Creating view for video=${newVideo} from fragment=$tag")

        // Create a new view
        val videoBinding = createVideoView(binding.container)
        bindVideo(videoBinding, newVideo)

        // Bind surfaceView when view is visible to user
        if (isViewVisible) {
          bindSurfaceView(videoBinding, newVideo)
        }

        binding.container.addView(videoBinding.root)
        newRenderedViews.add(RenderedViewPair(videoBinding, newVideo))
      }
    }

    crashlyticsLog(
      TAG,
      "updateVideos: Change grid items from ${renderedViews.size} -> ${newRenderedViews.size}"
    )

    renderedViews.clear()
    renderedViews.addAll(newRenderedViews)

    updateGridLayoutDimensions()
  }

  private fun bindSurfaceView(binding: GridItemVideoBinding, item: MeetingTrack) {
    SurfaceViewRendererUtil.bind(binding.videoCard.surfaceView, item, "fragment=$tag").let {
      if (it) binding.videoCard.surfaceView.visibility = View.VISIBLE
    }
  }

  private fun bindVideo(binding: GridItemVideoBinding, item: MeetingTrack) {
    // FIXME: Add a shared VM with activity scope to subscribe to events
    // binding.container.setOnClickListener { viewModel.onVideoItemClick?.invoke(item) }

    binding.videoCard.apply {
      name.text = item.mediaId
      nameInitials.text = NameUtils.getInitials(item.mediaId)
      iconScreenShare.visibility = if (item.isScreen) View.VISIBLE else View.GONE
      iconVideoOff.visibility = if (item.video != null) View.VISIBLE else View.GONE

      // TODO: Add listener for video stream on/off -> Change visibility of surface renderer
      surfaceView.apply {
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
        setEnableHardwareScaler(true)
      }
    }
  }

  private fun unbindSurfaceView(binding: GridItemVideoBinding, item: MeetingTrack) {
    SurfaceViewRendererUtil.unbind(binding.videoCard.surfaceView, item, "fragment=$tag").let {
      if (it) binding.videoCard.surfaceView.visibility = View.INVISIBLE
    }
  }

  private fun createVideoView(parent: ViewGroup): GridItemVideoBinding {
    return GridItemVideoBinding.inflate(
      LayoutInflater.from(context),
      parent,
      false
    )
  }

  override fun onResume() {
    super.onResume()
    crashlyticsLog(TAG, "Fragment=$tag onResume()")
    isViewVisible = true

    renderedViews.forEach {
      bindSurfaceView(it.binding, it.video)
    }
  }

  override fun onPause() {
    super.onPause()
    crashlyticsLog(TAG, "Fragment=$tag onPause()")
    isViewVisible = false

    renderedViews.forEach {
      unbindSurfaceView(it.binding, it.video)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    crashlyticsLog(TAG, "Fragment=$tag onDestroy()")

    // Release all references to views
    renderedViews.clear()
  }
}