package live.hms.android100ms.ui.meeting.videogrid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.android100ms.databinding.FragmentGridVideoBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.MeetingViewModel
import live.hms.android100ms.ui.meeting.MeetingViewModelFactory
import live.hms.android100ms.util.ROOM_DETAILS
import live.hms.android100ms.util.viewLifecycle

class VideoGridFragment : Fragment() {
  companion object {
    private const val TAG = "VideoGridFragment"
  }

  private var binding by viewLifecycle<FragmentGridVideoBinding>()

  private lateinit var clipboard: ClipboardManager

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    clipboard = requireActivity()
      .getSystemService(Context.CLIPBOARD_SERVICE)
        as ClipboardManager
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentGridVideoBinding.inflate(inflater, container, false)
    initVideoGrid()
    initVideoView()
    return binding.root
  }


  private fun initVideoGrid() {
    binding.viewPagerVideoGrid.apply {
      offscreenPageLimit = 1
      adapter = VideoGridAdapter(this@VideoGridFragment) { video ->
        Log.v(TAG, "onVideoItemClick: $video")

        Snackbar.make(
          binding.root,
          "Name: ${video.peer.userName} (${video.peer.role}) \nId: ${video.peer.customerUserId}",
          Snackbar.LENGTH_LONG,
        ).setAction("Copy") {
          val clip = ClipData.newPlainText("Customer Id", video.peer.customerUserId)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(
            requireContext(),
            "Copied customer id of ${video.peer.userName} to clipboard",
            Toast.LENGTH_SHORT
          ).show()
        }.show()
      }

      TabLayoutMediator(binding.tabLayoutDots, this) { _, _ ->
        // No text to be shown
      }.attach()
    }
  }

  private fun initVideoView() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      val adapter = binding.viewPagerVideoGrid.adapter as VideoGridAdapter
      adapter.setItems(tracks)
      Log.d(TAG, "Updated video-grid items: size=${tracks.size}")
    }
  }
}