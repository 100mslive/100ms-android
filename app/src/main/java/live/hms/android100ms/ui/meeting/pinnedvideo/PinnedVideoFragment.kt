package live.hms.android100ms.ui.meeting.pinnedvideo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.android100ms.databinding.FragmentPinnedVideoBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.ui.meeting.MeetingViewModel
import live.hms.android100ms.ui.meeting.MeetingViewModelFactory
import live.hms.android100ms.util.*
import org.webrtc.RendererCommon

class PinnedVideoFragment : Fragment() {

  companion object {
    private const val TAG = "PinnedVideoFragment"
  }

  private var pinnedTrack: MeetingTrack? = null

  private val videoListAdapter = VideoListAdapter() { changePinViewVideo(it) }

  private var binding by viewLifecycle<FragmentPinnedVideoBinding>()

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  // Determined using the onResume() and onPause()
  private var isViewVisible = false

  override fun onResume() {
    super.onResume()
    Log.d(TAG, "onResume()")

    isViewVisible = true
    handleOnPinVideoVisibilityChange()

    binding.recyclerViewVideos.adapter = videoListAdapter
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "onPause()")

    isViewVisible = false
    handleOnPinVideoVisibilityChange()

    // Detaching the recycler view adapter calls [RecyclerView.Adapter::onViewDetachedFromWindow]
    // which performs the required cleanup of the ViewHolder (Releases SurfaceViewRenderer Egl.Context)
    binding.recyclerViewVideos.adapter = null
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    Log.d(TAG, "onCreateView($inflater, $container, $savedInstanceState)")
    binding = FragmentPinnedVideoBinding.inflate(inflater, container, false)
    initRecyclerView()
    initPinnedView()
    initViewModels()
    return binding.root
  }

  private fun initPinnedView() {
    binding.pinVideo.surfaceView.apply {
      setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
      setEnableHardwareScaler(true)
    }

    updatePinnedVideoText()
  }

  private fun initRecyclerView() {
    binding.recyclerViewVideos.apply {
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }
  }

  private fun updatePinnedVideoText() {
    val name = pinnedTrack?.peer?.userName ?: ""
    binding.pinVideo.name.text = name
    binding.pinVideo.nameInitials.text = NameUtils.getInitials(name)
  }

  private fun handleOnPinVideoVisibilityChange() {
    crashlyticsLog(TAG, "handleOnPinVideoVisibilityChange: isViewVisible=${isViewVisible}")

    pinnedTrack?.let { track ->
      binding.pinVideo.surfaceView.apply {
        if (isViewVisible) {
          SurfaceViewRendererUtil.bind(this, track).let { success ->
            if (success) visibility = View.VISIBLE
          }
        } else {
          SurfaceViewRendererUtil.unbind(this, track)
          visibility = View.GONE
        }
      }

    }
  }

  @MainThread
  private fun changePinViewVideo(track: MeetingTrack) {
    if (track == pinnedTrack) {
      crashlyticsLog(TAG, "Track=$track is already pinned")
      return
    }

    crashlyticsLog(TAG, "Changing pin-view video to $track (previous=$pinnedTrack)")
    binding.pinVideo.surfaceView.apply {
      if (isViewVisible) {
        // Unbind and Bind only when the view is only released() / init() respectively
        pinnedTrack?.let {
          SurfaceViewRendererUtil.unbind(this, it)
          visibility = View.GONE
        }

        SurfaceViewRendererUtil.bind(this, track).let { success ->
          if (success) visibility = View.VISIBLE
        }
      }
    }

    pinnedTrack = track
    updatePinnedVideoText()
  }

  private fun initViewModels() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      val found = tracks.any { it == pinnedTrack }
      if (!found && tracks.isNotEmpty()) {
        changePinViewVideo(tracks[0])
      }

      videoListAdapter.setItems(tracks)
      Log.d(TAG, "Updated video-list items: size=${tracks.size}")
    }
  }
}