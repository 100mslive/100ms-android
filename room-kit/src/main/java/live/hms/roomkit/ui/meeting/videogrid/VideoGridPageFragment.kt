package live.hms.roomkit.ui.meeting.videogrid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import live.hms.roomkit.databinding.FragmentVideoGridPageBinding
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.commons.VideoGridBaseFragment
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.setBackgroundAndColor
import live.hms.roomkit.util.viewLifecycle
import kotlin.math.min

class VideoGridPageFragment : VideoGridBaseFragment() {

  companion object {
    private const val TAG = "VideoGridPageFragment"

    private const val BUNDLE_PAGE_INDEX = "bundle-page-index"
    private const val BUNDLE_IS_SCREEN_SHARE = "bundle-is-screen-share"

    fun newInstance(pageIndex: Int, isScreenShare: Boolean): VideoGridPageFragment {
      return VideoGridPageFragment().apply {
        arguments = bundleOf(
          BUNDLE_PAGE_INDEX to pageIndex,
          BUNDLE_IS_SCREEN_SHARE to isScreenShare)
      }
    }
  }


  private var binding by viewLifecycle<FragmentVideoGridPageBinding>()
  private val pageIndex by lazy { requireArguments()[BUNDLE_PAGE_INDEX] as Int }
  private val isScreenShare by lazy { requireArguments()[BUNDLE_IS_SCREEN_SHARE] as Boolean }

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

  private fun refreshGridRowsAndColumns(rowCount: Int, columnCount: Int) {
      //update row and columns span useful when remote screen share
      // remote screen share [enabled] =  3 * 2 --> 1 * 2
      // remote screen share [disabled] = 1 * 2 --> 3 * 2
      val shouldUpdate = shouldUpdateRowOrGrid(rowCount, columnCount)
      //don't update if row and column are same
      if (shouldUpdate.not()) return
      setVideoGridRowsAndColumns(rowCount, columnCount)
    renderCurrentPage(emptyList())
    meetingViewModel.speakerUpdateLiveData.refresh(rowCount, columnCount)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (isScreenShare)
      setVideoGridRowsAndColumns(1,1)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.container.setBackgroundAndColor(
      HMSPrebuiltTheme.getColours()?.backgroundDefault,
      HMSPrebuiltTheme.getDefaults().background_default
    )
  }
  override fun initViewModels() {
    super.initViewModels()

    if (isScreenShare.not()){
      meetingViewModel.updateRowAndColumnSpanForVideoPeerGrid.observe(viewLifecycleOwner) { (rowCount, columnCount) ->
        refreshGridRowsAndColumns(rowCount, columnCount)
      }
    }


    if (isScreenShare.not()) {
      meetingViewModel.speakerUpdateLiveData.observe(viewLifecycleOwner) { videoGridTrack ->
        renderCurrentPage(videoGridTrack)
      }
    } else {
      meetingViewModel.tracks.observe(viewLifecycleOwner) { track ->
        synchronized(meetingViewModel._tracks) {
          val screenShareTrack = track.filter { it.isScreen  }.toList()
          renderCurrentPage(screenShareTrack, isForceUpdate = true)

        }
      }
    }

    if (isScreenShare.not()) {
      meetingViewModel.activeSpeakers.observe(viewLifecycleOwner) { (videos, speakers) ->
        // Active speaker should be updated via, tracks AND actual active speakers.
        applySpeakerUpdates(speakers)
      }
    }

    //Don't register listener if it's not screen share


    //meetingViewModel.speakers.observe(viewLifecycleOwner) { applySpeakerUpdates(it) }
  }

  override fun isScreenshare(): Boolean {
    return isScreenShare
  }

  private fun renderCurrentPage(tracks: List<MeetingTrack>, isForceUpdate : Boolean = false) {
    val videos = getCurrentPageVideos(tracks)
    updateVideos(binding.container, videos, false, isForceUpdate = isForceUpdate)
  }
}