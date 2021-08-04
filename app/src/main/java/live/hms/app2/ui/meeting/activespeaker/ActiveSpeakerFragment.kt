package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.commons.VideoGridBaseFragment
import live.hms.app2.util.viewLifecycle
import org.webrtc.RendererCommon

class ActiveSpeakerFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "ActiveSpeakerFragment"
  }

  private data class LruItem(
    val peerId: String
  )

  private val lru by lazy { ActiveSpeakerLRU<LruItem>(maxItems) }
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
    val order = lru.getItemsInOrder()
    val videos = Array(order.size) { idx ->
      meetingViewModel.findTrack { it.peer.peerID == order[idx].peerId && it.isScreen.not() }
    }
    updateVideos(binding.container, videos)
  }

  private fun initViewModels() {
    lru.update(meetingViewModel.mapTracks { if (it.isScreen) null else LruItem(it.peer.peerID) })
    update()

    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      Log.v(TAG, "tracks update received 🎼 [size=${tracks.size}]")
      synchronized(tracks) {
        // Update lru just to keep it as much filled as possible

        val required = lru.capacity // We'd always want enough to fill the lru
        val all = tracks
          .sortedByDescending {
            if(it.audio == null || it.isScreen){
              it.peer.name.hashCode() - 100
            }
            else
              it.peer.name.hashCode()
          }.take(required)
          .map {
            LruItem(it.peer.peerID)
          }
        Log.i(TAG, "$all")

        lru.update(all)

        // Check for tracks not present
        val order = lru.getItemsInOrder()
        val toRemove = order.filter { item ->
          tracks.find {
            it.isScreen.not() && it.peer.peerID == item.peerId
          } == null
        }
        lru.remove(toRemove)
        update()

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
          if (isViewVisible) {
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

    meetingViewModel.speakers.observe(viewLifecycleOwner) { speakers ->
      Log.v(TAG, "speakers update received 🎙 [size=${speakers.size}]")
      lru.update(speakers.map { LruItem(it.peerId) })
      update()
      applySpeakerUpdates(speakers)
    }
  }
}