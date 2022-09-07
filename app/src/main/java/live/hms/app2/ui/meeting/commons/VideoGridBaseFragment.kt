package live.hms.app2.ui.meeting.commons

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.R
import live.hms.app2.databinding.GridItemVideoBinding
import live.hms.app2.databinding.VideoCardBinding
import live.hms.app2.helpers.NetworkQualityHelper
import live.hms.app2.ui.meeting.CustomPeerMetadata
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.meeting.pinnedvideo.StatsInterpreter
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSRemoteVideoTrack
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.ceil
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
  var isFragmentVisible = false
    private set

  private var wasLastModePip = false
  private var wasLastSpeakingViewIndex = 0
  private lateinit var gridLayout : GridLayout

  data class RenderedViewPair(
    val binding: GridItemVideoBinding,
    val meetingTrack: MeetingTrack,
    val statsInterpreter: StatsInterpreter?,
  )

  private val bindedVideoTrackIds = mutableSetOf<String>()
  protected val renderedViews = ArrayList<RenderedViewPair>()
  private val mediaPlayerManager by lazy { MediaPlayerManager(lifecycle) }

  //Normal layout
  private fun getNormalLayoutRowCount() = min(max(1, renderedViews.size), settings.videoGridRows)
  private fun getNormalLayoutColumnCount(): Int
    {
      val maxColumns = settings.videoGridColumns
      val result = max(1, (renderedViews.size + getNormalLayoutRowCount() - 1) / getNormalLayoutRowCount())
      if (result > maxColumns) {
        val videos = renderedViews.map { it.meetingTrack }
        throw IllegalStateException(
          "At most ${settings.videoGridRows * maxColumns} videos are allowed. Provided $videos"
        )
      }
      return result
    }

  private fun getPipLayoutRowCount() = max(1, ceil(renderedViews.size/2.0).toInt())
  private fun getPipLayoutColumnCount(): Int = min(renderedViews.size, 2)


  protected val maxItems: Int
    get() = settings.videoGridRows * settings.videoGridColumns

  private fun updateGridLayoutDimensions(layout: GridLayout, isPipMode: Boolean) {

    var childIdx: Pair<Int, Int>? = null
    var colIdx = 0
    var rowIdx = 0

    layout.apply {

      fun normalLayout() {
        for (child in children) {
          childIdx = Pair(rowIdx, colIdx)

          val params = child.layoutParams as GridLayout.LayoutParams
          params.rowSpec = GridLayout.spec(rowIdx, 1, 1f)
          params.columnSpec = GridLayout.spec(colIdx, 1, 1f)

          if (colIdx + 1 == getNormalLayoutColumnCount()) {
            rowIdx += 1
            colIdx = 0
          } else {
            colIdx += 1
          }
        }
        // Forces maxIndex to be recalculated when rowCount/columnCount is set
        requestLayout()

        rowCount = getNormalLayoutRowCount()
        columnCount = getNormalLayoutColumnCount()
      }

      fun pipLayout() {

        for (child in children) {
          childIdx = Pair(rowIdx, colIdx)
          val params = child.layoutParams as GridLayout.LayoutParams
          params.rowSpec = GridLayout.spec(rowIdx, 1, 1f)
          params.columnSpec = GridLayout.spec(colIdx, 1, 1f)

          //
          if ((colIdx + 1) % 2 == 0) {
            rowIdx += 1
            colIdx = 0
          } else {
            colIdx += 1
          }
        }

        requestLayout()

        rowCount = getNormalLayoutRowCount()
        columnCount = getPipLayoutColumnCount()
      }

        normalLayout()

      }

    hideOrShowGridsForPip(wasLastSpeakingViewIndex)

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
    Log.d(TAG,"bindSurfaceView for :: ${item.peer.name}")
    val earlyExit = item.video == null
            || item.video?.isMute == true
            || bindedVideoTrackIds.contains(item.video?.trackId)
    if (earlyExit) return

    binding.surfaceView.let { view ->
      view.setScalingType(scalingType)
      view.setEnableHardwareScaler(true)

      SurfaceViewRendererUtil.bind(view, item).let { success ->
        if (success) {
          binding.surfaceView.visibility = if (item.video?.isDegraded == true ) View.INVISIBLE else View.VISIBLE
          bindedVideoTrackIds.add(item.video!!.trackId)
          binding.surfaceView.setOnLongClickListener {
            (it as? SurfaceViewRenderer)?.let { surfaceView -> openDialog(surfaceView, item.video, item.peer.name.orEmpty()) }
            true
          }
        }
      }
    }
  }

  private fun openDialog(
    surfaceView: SurfaceViewRenderer?,
    videoTrack: HMSVideoTrack?,
    peerName: String
  ) {

    contextSafe { context, activity ->
      context.showTileListDialog (
        isLocalTrack = videoTrack is HMSLocalVideoTrack,
        onScreenCapture = { captureVideoFrame(surfaceView, videoTrack) },
        onSimulcast = { context.showSimulcastDialog(videoTrack as? HMSRemoteVideoTrack) }
      )
    }
  }



  private fun captureVideoFrame(surfaceView: SurfaceViewRenderer?, videoTrack: HMSVideoTrack?) {

    //safe check incase video
    if (videoTrack.isValid().not()){
      return
    }

    contextSafe { context, activity -> mediaPlayerManager.startPlay(R.raw.camera_shut, context )}
    surfaceView?.vibrateStrong()
    surfaceView?.addFrameListener(object : EglRenderer.FrameListener{
      override fun onFrame(bitmap: Bitmap?) {

        //this is returning on the render thread
        contextSafe { context, activity ->
          //stores the bitmap in local cache thus avoiding any permission
          val uri = bitmap?.saveCaptureToLocalCache(context)
          //the uri is used to open share intent
          uri?.let { activity.openShareIntent(it) }
        }

        //can't call on render thread this is important and just capture a single frame
        activity?.runOnUiThread { surfaceView?.removeFrameListener(this) }
      }
    }, 1.0f)
  }

  protected fun bindVideo(binding: VideoCardBinding, item: MeetingTrack) {
    // FIXME: Add a shared VM with activity scope to subscribe to events
    // binding.container.setOnClickListener { viewModel.onVideoItemClick?.invoke(item) }

    binding.apply {
      // Donot update the text view if not needed, this causes redraw of the entire view leading to  flicker
      if (name.text.equals(item.peer.name).not()) {
        name.text = item.peer.name
        nameInitials.text = NameUtils.getInitials(item.peer.name)
      }
      // Using alpha instead of visibility to stop redraw of the entire view to stop flickering
      iconScreenShare.alpha = visibilityOpacity( (item.isScreen) )
      iconAudioOff.alpha = visibilityOpacity(
        item.isScreen.not() &&
            (item.audio == null || item.audio!!.isMute)
      )
      icDegraded.alpha = visibilityOpacity(item.video?.isDegraded == true)

      /** [View.setVisibility] */
      val surfaceViewVisibility = if (item.video == null
        || item.video?.isMute == true
        || item.video?.isDegraded == true) {
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
    Log.d(TAG,"unbindSurfaceView for :: ${item.peer.name}")
    if (!bindedVideoTrackIds.contains(item.video?.trackId ?: "")) return

    SurfaceViewRendererUtil.unbind(binding.surfaceView, item, metadata).let {
      if (it) {
        binding.surfaceView.setOnLongClickListener(null)
        binding.surfaceView.visibility = View.INVISIBLE
        bindedVideoTrackIds.remove(item.video!!.trackId)
      }
    }
  }


  protected fun updateVideos(
    layout: GridLayout,
    newVideos: List<MeetingTrack?>,
    isVideoGrid: Boolean
  ) {
    crashlyticsLog(
      TAG,
      "updateVideos(${newVideos.size}) -- presently ${renderedViews.size} items in grid"
    )
    gridLayout = layout
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
          if (isFragmentVisible) unbindSurfaceView(
            currentRenderedView.binding.videoCard,
            currentRenderedView.meetingTrack
          )
//          currentRenderedView.statsInterpreter?.close()
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

          if (isFragmentVisible && !bindedVideoTrackIds.contains(newVideo.video?.trackId ?: "")) {
            // This view is not yet initialized (possibly because when AudioTrack was added --
            // VideoTrack was not present, hence had to create an empty tile)
            bindSurfaceView(renderedViewPair.binding.videoCard, newVideo)
          }
          val downlinkScore = newVideo.peer.networkQuality?.downlinkQuality
          updateNetworkQualityView(downlinkScore ?: -1,requireContext(),renderedViewPair.binding.videoCard.networkQuality)

          renderedViewPair.binding.videoCard.raisedHand.alpha =
            visibilityOpacity(CustomPeerMetadata.fromJson(newVideo.peer.metadata)?.isHandRaised == true)
        } else {
          crashlyticsLog(TAG, "updateVideos: Creating view for video=${newVideo} in fragment=$tag")
          requiresGridLayoutUpdate = true

          // Create a new view
          val videoBinding = createVideoView(layout)
          var statsInterpreter: StatsInterpreter? = null
          if (!isVideoGrid) {
            statsInterpreter = StatsInterpreter(settings.showStats)
            statsInterpreter.initiateStats(
              this,
              meetingViewModel.getStats(),
              newVideo.video,
              newVideo.audio,
              newVideo.peer.isLocal
            ) { videoBinding.videoCard.statsView.text = it }
          }

          // Bind surfaceView when view is visible to user
          if (isFragmentVisible) {
            bindSurfaceView(videoBinding.videoCard, newVideo)
          }

          videoBinding.videoCard.raisedHand.alpha =
            visibilityOpacity(CustomPeerMetadata.fromJson(newVideo.peer.metadata)?.isHandRaised == true)
          layout.addView(videoBinding.root)
          newRenderedViews.add(RenderedViewPair(videoBinding, newVideo, statsInterpreter))
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
      updateGridLayoutDimensions(layout, isPipMode = false)
    }
  }

  private fun applyMetadataUpdates(peerTypePair: Pair<HMSPeer, HMSPeerUpdate>) {
    val isUpdatedPeerRendered =
      renderedViews.find { it.meetingTrack.peer.peerID == peerTypePair.first.peerID }
    if (isUpdatedPeerRendered != null) {
      when (peerTypePair.second) {
        HMSPeerUpdate.METADATA_CHANGED -> {
          val isHandRaised =
            CustomPeerMetadata.fromJson(isUpdatedPeerRendered.meetingTrack.peer.metadata)?.isHandRaised == true
          isUpdatedPeerRendered.binding.videoCard.raisedHand.alpha = visibilityOpacity(isHandRaised)
        }
        HMSPeerUpdate.NAME_CHANGED -> {
          with(isUpdatedPeerRendered.binding.videoCard) {
            name.text = isUpdatedPeerRendered.meetingTrack.peer.name
            nameInitials.text = NameUtils.getInitials(isUpdatedPeerRendered.meetingTrack.peer.name)
          }
        }
        HMSPeerUpdate.NETWORK_QUALITY_UPDATED -> {
          val downlinkScore = peerTypePair.first.networkQuality?.downlinkQuality
          isUpdatedPeerRendered.binding.videoCard.networkQuality.apply {
            updateNetworkQualityView(downlinkScore ?: -1,requireContext(),this)
          }
        }
      }
    }
  }

  fun updateNetworkQualityView(downlinkScore : Int,context: Context,imageView: ImageView){
    NetworkQualityHelper.getNetworkResource(downlinkScore, context = requireContext()).let { drawable ->
      if (downlinkScore == 0) {
        imageView.setColorFilter(ContextCompat.getColor(context, R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
      } else {
        imageView.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light), android.graphics.PorterDuff.Mode.SRC_IN)
      }
      imageView.setImageDrawable(drawable)
      if (drawable == null){
        imageView.visibility = View.GONE
      }else{
        imageView.visibility = View.VISIBLE
      }
    }
  }

  protected fun applySpeakerUpdates(speakers: Array<HMSSpeaker>) {
    renderedViews.forEachIndexed { index, renderedView ->
      val track = renderedView.meetingTrack.audio
      renderedView.binding.apply {
        if (track == null || track.isMute) {
          videoCard.audioLevel.apply {
            text = "-"
          }
          container.strokeWidth = 0
        } else {
          val level = speakers.find { it.hmsTrack?.trackId == track.trackId }?.level ?: 0

          videoCard.audioLevel.apply {
            text = "$level"
          }
          if (level >= settings.silenceAudioLevelThreshold) {
            hideOrShowGridsForPip(index)
            wasLastSpeakingViewIndex = index
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

  /**
   * When onlyIndexToShow has a value it'll show the most active speaker only in pip mode
   */
  private fun hideOrShowGridsForPip(onlyIndexToShow : Int? = null) {
    var showAtleastOne = false
    if (activity?.isInPictureInPictureMode == true && onlyIndexToShow != null && renderedViews.size > 0) {
      renderedViews.forEachIndexed { index, renderedViewPair ->
        if (onlyIndexToShow == index && renderedViewPair.binding.root.isVisible.not()) {
          renderedViewPair.binding.root.visibility = View.VISIBLE
          showAtleastOne = true
        } else if (onlyIndexToShow != index && renderedViewPair.binding.root.isVisible) {
          renderedViewPair.binding.root.visibility = View.GONE
        }
      }
      if (showAtleastOne.not()) {
        renderedViews[0].binding.root.visibility = View.VISIBLE
      }
    } else {
      renderedViews.forEachIndexed { index, renderedViewPair ->
        if (renderedViewPair.binding.root.isVisible.not())
          renderedViewPair.binding.root.visibility = View.VISIBLE
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (wasLastModePip) {
      //force pip mode layout refresh
      hideOrShowGridsForPip(null)
      if (::gridLayout.isInitialized)
        updateGridLayoutDimensions(gridLayout, isPipMode = false)
      wasLastModePip = false
      return
    }
    crashlyticsLog(TAG, "Fragment=$tag onResume()")
    isFragmentVisible = true
    bindViews()
  }

  fun bindViews() {
    renderedViews.forEach {
      bindSurfaceView(it.binding.videoCard, it.meetingTrack)
      it.statsInterpreter?.initiateStats(
        this,
        meetingViewModel.getStats(),
        it.meetingTrack.video,
        it.meetingTrack.audio,
        it.meetingTrack.peer.isLocal
      ) { string -> it.binding.videoCard.statsView.text = string }
    }
  }

  override fun onPause() {
    super.onPause()
    crashlyticsLog(TAG, "Fragment=$tag onPause()")
    if (activity?.isInPictureInPictureMode == true) {
      wasLastModePip = true
      //force pip mode layout refresh
      hideOrShowGridsForPip(wasLastSpeakingViewIndex)
    } else {
      isFragmentVisible = false
      unbindViews()
    }

  }

  fun unbindViews() {
    renderedViews.forEach {
      unbindSurfaceView(it.binding.videoCard, it.meetingTrack)
//      it.statsInterpreter?.close()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    crashlyticsLog(TAG, "Fragment=$tag onDestroy()")
    if (wasLastModePip) {
       unbindViews()
    }
    // Release all references to views
    renderedViews.clear()
  }

  @CallSuper
  open fun initViewModels() {
    meetingViewModel.peerMetadataNameUpdate.observe(viewLifecycleOwner) {
      applyMetadataUpdates(it)
    }
  }
}