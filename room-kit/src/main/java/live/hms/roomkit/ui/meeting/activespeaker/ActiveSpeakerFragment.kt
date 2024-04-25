package live.hms.roomkit.ui.meeting.activespeaker

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.commons.VideoGridBaseFragment
import live.hms.roomkit.ui.meeting.pinnedvideo.StatsInterpreter
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.*
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSRemoteVideoTrack
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.utils.HMSLogger
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.ConcurrentLinkedDeque

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
    binding.applyTheme()
    screenShareStats = StatsInterpreter(settings.showStats)
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

    screenShareTrack?.let { meetingTrack ->
      binding.screenShare.raisedHand.alpha =
        visibilityOpacity(meetingTrack.peer.isHandRaised)
      bindSurfaceView(
        binding.screenShare,
        meetingTrack,
        RendererCommon.ScalingType.SCALE_ASPECT_FIT
      )
      binding.screenShare.hmsVideoView.setOnLongClickListener { view ->
        openDialog(
          view as? SurfaceViewRenderer,
          meetingTrack.video,
          meetingTrack.peer.name.orEmpty()
        )
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
      context.showTileListDialog (
        isLocalTrack = videoTrack is HMSLocalVideoTrack,
        onScreenCapture = { captureVideoFrame(surfaceView, videoTrack) },
        onSimulcast = { context.showSimulcastDialog(videoTrack as? HMSRemoteVideoTrack) },
        onMirror = { context.showMirrorOptions(surfaceView)}
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true) {
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
    binding.container.visibility = if (screenShareTrack != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true) {
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
            HMSLogger.v(TAG,"metadata changed : ${it.second} ")
          }

          HMSPeerUpdate.NAME_CHANGED -> {
            binding.screenShare.name.text = it.first.name
          }
          else -> {}
        }
      }
    }

    meetingViewModel.trackStatus.observe(viewLifecycleOwner) { statsPair ->
      if (statsPair.second){
        binding.screenShare.statsView.visibility = View.GONE
      }else{
        binding.screenShare.statsView.visibility = View.VISIBLE
        binding.screenShare.statsView.text = statsPair.first
      }
    }
  }

  override fun isScreenshare(): Boolean {
   return false
  }

  private var screenShareStats : StatsInterpreter? = null
  private fun updateScreenshareTracks(tracks: ConcurrentLinkedDeque<MeetingTrack>) {

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
       screenShareStats?.initiateStats(
          viewLifecycleOwner,
          meetingViewModel.getStats(),
          screen.video,
          screen.audio, screen.peer.isLocal
        ) { statsString ->
            binding.screenShare.statsView.text = statsString
        }
      meetingViewModel.statsToggleData.observe(viewLifecycleOwner, Observer {
        if (it){
          binding.screenShare.statsView.visibility = View.VISIBLE
        }else{
          binding.screenShare.statsView.visibility = View.GONE
        }
      })
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
        raisedHand.alpha = visibilityOpacity(screen.peer.isHandRaised)
      }
      binding.screenShareContainer.visibility = View.VISIBLE
    }

    if (screenShareTrack == null && binding.screenShareContainer.visibility != View.GONE) {
      binding.screenShareContainer.visibility = View.GONE
    }
  }
}