package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.commons.VideoGridBaseFragment
import live.hms.app2.util.viewLifecycle
import live.hms.video.utils.HMSLogger
import org.webrtc.RendererCommon

class ActiveSpeakerFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "ActiveSpeakerFragment"
  }

  private data class LruItem(
    val peerId: String,
    val peerName: String
  ) {
    override fun toString(): String {
      return peerName
    }
  }

  private val lru by lazy { ActiveSpeakerCache<LruItem>(4) }
  private var binding by viewLifecycle<FragmentActiveSpeakerBinding>()

  private var screenShareTrack: MeetingTrack? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)

    initViewModels()
    return binding.root
  }

  override fun onResume() {
    super.onResume()
    screenShareTrack?.let {
      bindSurfaceView(binding.screenShare, it, RendererCommon.ScalingType.SCALE_ASPECT_FIT)
    }
  }

  override fun onPause() {
    super.onPause()
    screenShareTrack?.let {
      unbindSurfaceView(binding.screenShare, it)
    }
  }

  private fun update() {
    val order = lru.getAllItems()
    val videos = Array(order.size) { idx ->
      meetingViewModel.findTrack { it.peer.peerID == order[idx].peerId && it.isScreen.not() }
    }
    updateVideos(binding.container, videos)
  }

  private fun initViewModels() {

    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      HMSLogger.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      trackUpdateLruTrigger(tracks)
      updateScreenshareTracks(tracks)
    }

    meetingViewModel.speakers.observe(viewLifecycleOwner) { speakers ->
      HMSLogger.v(
        TAG,
        "speakers update received 🎙 [size=${speakers.size}, names=${speakers.map { it.peer?.name }}] "
      )

      lru.update(speakers.map { LruItem(it.peer!!.peerID, it.peer!!.name) }, true)
      update()
      // Active speaker should be updated via, tracks AND actual active speakers.
      applySpeakerUpdates(speakers)
    }
  }

  private fun trackUpdateLruTrigger(tracks: List<MeetingTrack>) {
    synchronized(tracks) {
      // Update lru just to keep it as much filled as possible

      val required = lru.capacity // We'd always want enough to fill the lru
      val all = tracks
        .sortedByDescending {
          if (it.audio == null || it.audio?.isMute == true || it.isScreen) {
            it.peer.name.hashCode() * -1 // Drop these ids really low.
          } else
            it.peer.name.hashCode()
        }.take(required)
        .map {
          LruItem(it.peer.peerID, it.peer.name)
        }

      lru.update(all, false)

      update()
    }
  }

  private fun updateScreenshareTracks(tracks: List<MeetingTrack>) {

    // Check if the currently shared screen-share track is removed
    screenShareTrack?.let { screen ->
      if (tracks.find { screen == it } == null) {
        screenShareTrack?.let { unbindSurfaceView(binding.screenShare, it) }
        screenShareTrack = null
      }
    }

    // Check for screen share
    if (screenShareTrack == null) tracks.find { it.isScreen }?.let { screen ->
      screenShareTrack = screen
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
      }
      binding.screenShareContainer.visibility = View.VISIBLE
    }

    if (screenShareTrack == null && binding.screenShareContainer.visibility != View.GONE) {
      binding.screenShareContainer.visibility = View.GONE
    }
  }
}