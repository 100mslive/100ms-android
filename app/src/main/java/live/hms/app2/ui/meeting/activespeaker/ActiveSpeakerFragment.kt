package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.commons.VideoGridBaseFragment
import live.hms.app2.util.viewLifecycle

class ActiveSpeakerFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "ActiveSpeakerFragment "
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
      bindSurfaceView(binding.screenShare, it)
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
      meetingViewModel.getTrackByPeerId(order[idx].peerId)
    }
    updateVideos(binding.container, videos)
  }

  private fun initViewModels() {
    lru.update(meetingViewModel.mapTracks { if (it.isScreen) null else LruItem(it.peer.peerID) })
    update()

    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      synchronized(tracks) {
        // Update lru just to keep it as much filled as possible
        if (lru.size < lru.capacity) {
          val required = lru.capacity - lru.size
          val all = tracks.mapNotNull {
            if (it.isScreen || it.audio != null) null
            else LruItem(it.peer.peerID)
          }

          val extra = ArrayList<LruItem>()
          val inLru = lru.getItemsInOrder()
          for (item in all) {
            if (inLru.find { item.peerId == it.peerId } == null) {
              extra.add(item)
            }
            if (extra.size == required) break
          }
          lru.update(extra)
        }

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
            bindSurfaceView(binding.screenShare, screen)
          }
          bindVideo(binding.screenShare, screen)
          binding.screenShare.apply {
            iconAudioOff.visibility = View.GONE
            iconScreenShare.visibility = View.GONE
          }
          binding.screenShareContainer.visibility = View.VISIBLE
        }

        if (screenShareTrack == null && binding.screenShareContainer.visibility != View.GONE) {
          binding.screenShareContainer.visibility = View.GONE
        }
      }
    }

    meetingViewModel.speakers.observe(viewLifecycleOwner) { speakers ->
      lru.update(speakers.map { LruItem(it.peerId) })
      update()
      applySpeakerUpdates(speakers)
    }
  }
}