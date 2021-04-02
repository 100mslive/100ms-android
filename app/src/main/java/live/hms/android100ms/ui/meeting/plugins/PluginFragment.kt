package live.hms.android100ms.ui.meeting.plugins

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.android100ms.databinding.FragmentPluginVideoBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.MeetingTrack
import live.hms.android100ms.ui.meeting.MeetingViewModel
import live.hms.android100ms.ui.meeting.MeetingViewModelFactory
import live.hms.android100ms.ui.meeting.pinnedvideo.VideoListAdapter
import live.hms.android100ms.util.*

class PluginFragment : Fragment() {

  companion object {
    private const val TAG = "PinnedVideoFragment"
  }

  private var pinnedTrack: MeetingTrack? = null

  private val videoListAdapter = VideoListAdapter {}

  private var binding by viewLifecycle<FragmentPluginVideoBinding>()

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

    binding.recyclerViewVideos.adapter = videoListAdapter
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "onPause()")

    isViewVisible = false

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
    binding = FragmentPluginVideoBinding.inflate(inflater, container, false)

    initRecyclerView()
    initPinnedView()
    initViewModels()
    return binding.root
  }

  private fun initPinnedView() {
    binding.webview.settings.javaScriptEnabled = true
  }

  private fun initRecyclerView() {
    val orientation =
      if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LinearLayoutManager.VERTICAL
      } else {
        LinearLayoutManager.HORIZONTAL
      }
    binding.recyclerViewVideos.apply {
      layoutManager = LinearLayoutManager(requireContext(), orientation, false)
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun initViewModels() {
    meetingViewModel.pluginData.observe(viewLifecycleOwner) {
      if (it != null) {
        binding.webview.loadUrl(it.url)
        binding.name.text = it.ownerName
        binding.iconLocked.visibility = if (it.isLocked) View.VISIBLE else View.GONE

        if (it.isLocked) {
          binding.webview.setOnTouchListener { _, _ -> true }
        } else {
          binding.webview.setOnTouchListener(null)
        }
      }
    }

    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      videoListAdapter.setItems(tracks)
      Log.d(TAG, "Updated video-list items: size=${tracks.size}")
    }
  }
}