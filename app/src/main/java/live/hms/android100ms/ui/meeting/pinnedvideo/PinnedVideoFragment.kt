package live.hms.android100ms.ui.meeting.pinnedvideo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.android100ms.databinding.FragmentPinnedVideoBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.ui.meeting.MeetingViewModel
import live.hms.android100ms.ui.meeting.MeetingViewModelFactory
import live.hms.android100ms.util.*

class PinnedVideoFragment(
  initialPinnedTrack: MeetingTrack
) : Fragment() {

  companion object {
    private const val TAG = "PinnedVideoFragment"
  }

  private var pinnedTrack = initialPinnedTrack

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
    crashlyticsLog(TAG, "Fragment=$tag onResume()")
    isViewVisible = true

    binding.pinVideo.surfaceView.apply {
      SurfaceViewRendererUtil.bind(this, pinnedTrack).let { success ->
        visibility = if (success) View.VISIBLE else View.GONE
      }
    }
  }

  override fun onPause() {
    super.onPause()
    crashlyticsLog(TAG, "Fragment=$tag onPause()")
    isViewVisible = false

    binding.pinVideo.surfaceView.apply {
      SurfaceViewRendererUtil.unbind(this, pinnedTrack)
      visibility = View.GONE
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPinnedVideoBinding.inflate(inflater, container, false)
    initRecyclerView()
    initPinnedView()
    initViewModels()
    return binding.root
  }

  private fun initPinnedView() {
    updatePinnedVideoText()
    binding.pinVideo.surfaceView.apply {
      SurfaceViewRendererUtil.bind(this, pinnedTrack).let { success ->
        visibility = if (success) View.VISIBLE else View.GONE
      }
    }
  }

  private fun initRecyclerView() {
    binding.recyclerViewVideos.apply {
      layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
      adapter = VideoListAdapter() { updatePinViewVideo(it.track) }
    }
  }

  private fun updatePinnedVideoText() {
    val name = pinnedTrack.peer.userName
    binding.pinVideo.name.text = name
    binding.pinVideo.nameInitials.text = NameUtils.getInitials(name)
  }

  private fun updatePinViewVideo(track: MeetingTrack) {
    binding.pinVideo.surfaceView.apply {
      SurfaceViewRendererUtil.unbind(this, pinnedTrack)
      visibility = View.GONE

      pinnedTrack = track
      SurfaceViewRendererUtil.bind(this, pinnedTrack).let { success ->
        if (success) visibility = View.VISIBLE
      }
    }

    updatePinnedVideoText()
  }

  private fun initViewModels() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      // TODO: Check if pinned track is removed -- Handle it if removed!

      val adapter = binding.recyclerViewVideos.adapter as VideoListAdapter
      adapter.setItems(tracks)
      Log.d(TAG, "Update video-list items: size=${tracks.size}")
    }
  }
}