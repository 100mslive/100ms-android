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

  private fun initViewModels() {

    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      HMSLogger.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      updateScreenshareTracks(tracks)
    }

    meetingViewModel.activeSpeakersUpdatedTracks.observe(viewLifecycleOwner) { tracks ->
      HMSLogger.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      updateVideos(binding.container, tracks)
    }

    meetingViewModel.activeSpeakers.observe(viewLifecycleOwner) { (videos, speakers) ->
      updateVideos(binding.container, videos)
      // Active speaker should be updated via, tracks AND actual active speakers.
      applySpeakerUpdates(speakers)
    }
  }

  private fun updateScreenshareTracks(tracks: List<MeetingTrack>) {

    // Check if the currently shared screen-share track is removed
    screenShareTrack?.let { screen ->
      if (!tracks.contains(screen)) {
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