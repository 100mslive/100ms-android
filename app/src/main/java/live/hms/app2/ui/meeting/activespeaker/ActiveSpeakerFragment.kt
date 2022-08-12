package live.hms.app2.ui.meeting.activespeaker

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import live.hms.app2.R
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
import live.hms.app2.ui.meeting.CustomPeerMetadata
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.commons.VideoGridBaseFragment
import live.hms.app2.ui.meeting.pinnedvideo.StatsInterpreter
import live.hms.app2.util.*
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.utils.HMSLogger
import org.webrtc.EglRenderer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class ActiveSpeakerFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "ActiveSpeakerFragment"
  }

  private var binding by viewLifecycle<FragmentActiveSpeakerBinding>()

  private var screenShareTrack: MeetingTrack? = null
  private var wasLastModePip = false

  private val mediaPlayerManager by lazy { MediaPlayerManager(lifecycle) }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViewModels()
  }

  private fun unBindScreenShareTrack() {
    screenShareTrack?.let {
      unbindSurfaceView(binding.screenShare, it)
    }
  }

  override fun onResume() {
    if (wasLastModePip) {
      //if it's coming back from pip --> full screen i.e [pause --> resume] we wont' bind the screenshare track again since we never removed it in the first place
      super.onResume()
      wasLastModePip = false
      screenShareOverLocalVideoInGrid()
      return
    }

    screenShareTrack?.let {
      screenShareStats.initiateStats(
        this,
        meetingViewModel.getStats(),
        it.video,
        it.audio, it.peer.isLocal
      ) { statsString ->
        meetingViewModel?.updateTrackStatus(statsString)
      }
      binding.screenShare.raisedHand.alpha = visibilityOpacity(CustomPeerMetadata.fromJson(it.peer.metadata)?.isHandRaised == true)
      bindSurfaceView(binding.screenShare, it, RendererCommon.ScalingType.SCALE_ASPECT_FIT)
      binding.screenShare.surfaceView.setOnLongClickListener { view ->
        openDialog(view as? SurfaceViewRenderer, it.video, it.peer.name.orEmpty())
        return@setOnLongClickListener true
      }
    }
    super.onResume()
  }

  private fun openDialog(
    surfaceView: SurfaceViewRenderer?,
    videoTrack: HMSVideoTrack?,
    peerName: String
  ) {

    if (videoTrack.isValid().not())
      return
    contextSafe { context, activity ->
      context.showTileListDialog (peerName){ captureVideoFrame(surfaceView, videoTrack) }
    }

  }

  private fun captureVideoFrame(surfaceView: SurfaceViewRenderer?, videoTrack: HMSVideoTrack?) {

    //safe check incase video
    if (videoTrack.isValid().not()){
      return
    }
    contextSafe { context, activity -> mediaPlayerManager.startPlay(R.raw.camera_shut, context )}
    surfaceView?.vibrateStrong()

    surfaceView?.onBitMap(onBitmap = { bitmap ->
      contextSafe { context, activity ->
        //stores the bitmap in local cache thus avoiding any permission
        val uri = bitmap?.saveCaptureToLocalCache(context)
        //the uri is used to open share intent
        uri?.let { activity.openShareIntent(it) }
      }
    })
  }


  override fun onPause() {

    if (activity?.isInPictureInPictureMode == true) {
      //when it's pip mode don't unbind views
      wasLastModePip = true
      screenShareOverLocalVideoInGrid()
    } else {
     unBindScreenShareTrack()
    }
//    screenShareStats.close()
    super.onPause()
  }

  private fun screenShareOverLocalVideoInGrid() {
    //hide video grid when screen share is shown!
    binding.container.visibility = if (screenShareTrack != null && activity?.isInPictureInPictureMode == true) {
        View.GONE
    } else {
        View.VISIBLE
    }
  }

  override fun initViewModels() {
    super.initViewModels()
    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      HMSLogger.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      updateScreenshareTracks(tracks)
    }

    meetingViewModel.activeSpeakersUpdatedTracks.observe(viewLifecycleOwner) { tracks ->
      HMSLogger.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      updateVideos(binding.container, tracks, false)
    }

    meetingViewModel.activeSpeakers.observe(viewLifecycleOwner) { (videos, speakers) ->
      updateVideos(binding.container, videos, false)
      // Active speaker should be updated via, tracks AND actual active speakers.
      applySpeakerUpdates(speakers)
    }

    meetingViewModel.peerMetadataNameUpdate.observe(viewLifecycleOwner) {
      if( screenShareTrack?.peer?.peerID == it.first.peerID) {
        when(it.second) {
          HMSPeerUpdate.METADATA_CHANGED -> {
            binding.screenShare.raisedHand.alpha = visibilityOpacity(CustomPeerMetadata.fromJson(it.first.metadata)?.isHandRaised == true)
          }

          HMSPeerUpdate.NAME_CHANGED -> {
            binding.screenShare.name.text = it.first.name
          }
          else -> {}
        }
      }
    }

    meetingViewModel.trackStatus.observe(viewLifecycleOwner) { statsString ->
      binding.screenShare.statsView.text = statsString
    }
  }

  private val screenShareStats by lazy { StatsInterpreter(settings.showStats) }
  private fun updateScreenshareTracks(tracks: List<MeetingTrack>) {

    // Check if the currently shared screen-share track is removed
    screenShareTrack?.let { screen ->
      if (!tracks.contains(screen)) {
        screenShareTrack?.let { unbindSurfaceView(binding.screenShare, it) }
//        screenShareStats.close()
        screenShareTrack = null
        screenShareOverLocalVideoInGrid()
      }
    }

    // Check for screen share
    if (screenShareTrack == null) tracks.find { it.isScreen }?.let { screen ->
      screenShareStats.initiateStats(
        viewLifecycleOwner,
        meetingViewModel.getStats(),
        screen.video,
        screen.audio,
        screen.peer.isLocal
      ) {
        binding.screenShare.statsView.text = it
      }
      screenShareTrack = screen
      screenShareOverLocalVideoInGrid()
      if (isFragmentVisible) {
        bindSurfaceView(
          binding.screenShare,
          screen,
          RendererCommon.ScalingType.SCALE_ASPECT_FIT
        )
      }
      bindVideo(binding.screenShare, screen)
      binding.screenShare.apply {
        iconAudioOff.visibility = View.GONE
        iconScreenShare.visibility = View.GONE
        audioLevel.visibility = View.GONE
        raisedHand.alpha = visibilityOpacity(CustomPeerMetadata.fromJson(screen.peer.metadata)?.isHandRaised == true)
      }
      binding.screenShareContainer.visibility = View.VISIBLE
    }

    if (screenShareTrack == null && binding.screenShareContainer.visibility != View.GONE) {
      binding.screenShareContainer.visibility = View.GONE
    }
  }


}