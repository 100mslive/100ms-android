package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
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

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)

    initViewModels()
    return binding.root
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
      // Check for tracks not present
      val order = lru.getItemsInOrder()
      val toRemove = order.filter { item ->
        tracks.find {
          it.isScreen.not() && it.peer.peerID == item.peerId
        } == null
      }
      lru.remove(toRemove)
      update()
    }

    meetingViewModel.speakers.observe(viewLifecycleOwner) { speakers ->
      lru.update(speakers.map { LruItem(it.peerId) })
      update()
      applySpeakerUpdates(speakers)
    }
  }
}