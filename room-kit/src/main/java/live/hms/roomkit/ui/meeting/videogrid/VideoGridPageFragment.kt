package live.hms.roomkit.ui.meeting.videogrid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import live.hms.roomkit.databinding.FragmentVideoGridPageBinding
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.commons.VideoGridBaseFragment
import live.hms.roomkit.util.viewLifecycle
import kotlin.math.min

class VideoGridPageFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "VideoGridPageFragment"

    private const val BUNDLE_PAGE_INDEX = "bundle-page-index"

    fun newInstance(pageIndex: Int): VideoGridPageFragment {
      return VideoGridPageFragment().apply {
        arguments = bundleOf(BUNDLE_PAGE_INDEX to pageIndex)
      }
    }
  }


  private var binding by viewLifecycle<FragmentVideoGridPageBinding>()
  private val pageIndex by lazy { requireArguments()[BUNDLE_PAGE_INDEX] as Int }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentVideoGridPageBinding.inflate(inflater, container, false)

    initViewModels()
    return binding.root
  }

  override fun onResume() {
    super.onResume()
    // Turn of sorting when we leave the first page
    meetingViewModel.speakerUpdateLiveData.enableSorting(pageIndex == 0)
  }
  private fun getCurrentPageVideos(tracks: List<MeetingTrack>): List<MeetingTrack?> {
    val pageVideos = ArrayList<MeetingTrack?>()

    // Range is [fromIndex, toIndex] -- Notice the bounds
    val itemsCount = maxItems
    val fromIndex = pageIndex * itemsCount
    val toIndex = min(tracks.size, (pageIndex + 1) * itemsCount) - 1

    for (idx in fromIndex..toIndex step 1) {
      pageVideos.add(tracks[idx])
    }

    return pageVideos
  }

  override fun initViewModels() {
    super.initViewModels()
    meetingViewModel.speakerUpdateLiveData.observe(viewLifecycleOwner) { tracks ->
//      Log.d("VGPF","Tracks: ${tracks.size}, order: ${tracks.map { it.peer.name }}")
      val videos = getCurrentPageVideos(tracks)
      updateVideos(binding.container, videos, false)
    }

    //meetingViewModel.speakers.observe(viewLifecycleOwner) { applySpeakerUpdates(it) }
  }
}