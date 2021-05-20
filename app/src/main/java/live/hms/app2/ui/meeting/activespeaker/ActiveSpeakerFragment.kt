package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import live.hms.app2.databinding.FragmentActiveSpeakerBinding
import live.hms.app2.databinding.ListItemVideoBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.meeting.MeetingViewModelFactory
import live.hms.app2.util.*
import kotlin.properties.Delegates

class ActiveSpeakerFragment : Fragment() {

  companion object {
    private const val TAG = "ActiveSpeakerFragment "
  }

  private var pinnedTrack: MeetingTrack? = null
  private var binding by viewLifecycle<FragmentActiveSpeakerBinding>()

  private val lru = ActiveSpeakerLRU<String>(4)
  private var videoBindings: Array<ListItemVideoBinding> by Delegates.notNull()

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
  }

  override fun onPause() {
    super.onPause()
    Log.d(TAG, "onPause()")

    isViewVisible = false
    handleOnPinVideoVisibilityChange()
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


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    Log.d(TAG, "onCreateView($inflater, $container, $savedInstanceState)")
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)
    initVideoViews()
    initViewModels()
    return binding.root
  }

  private fun initVideoViews() {
    binding.apply {
      videoBindings = arrayOf(
        activeSpeakerVideo1,
        activeSpeakerVideo2,
        activeSpeakerVideo3
      )
    }

    binding.containerBottomTray.visibility = View.GONE
    videoBindings.forEach {
      it.surfaceView.visibility = View.GONE
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


  private fun updatePinnedVideoText() {
    pinnedTrack?.let { track ->
      val nameStr = track.peer.name
      val isScreen = track.isScreen
      binding.pinVideo.apply {
        name.text = nameStr
        nameInitials.text = NameUtils.getInitials(nameStr)
        iconScreenShare.visibility = if (isScreen) View.VISIBLE else View.GONE
        iconAudioOff.visibility = if (track.audio?.isMute != false) View.VISIBLE else View.GONE
      }
    }
  }

  private val hasVideo = Array<MeetingTrack?>(4) { null }

  private fun updateBottomTrayVideoText(tracks: Array<MeetingTrack>) {
    val trackIt = tracks.iterator()
    val bindingIt = videoBindings.iterator()
    var index = 0

    while (trackIt.hasNext() && bindingIt.hasNext()) {
      val binding = bindingIt.next()
      val track = trackIt.next()

      hasVideo[index]?.let { oldTrack ->
        binding.surfaceView.apply {
          SurfaceViewRendererUtil.unbind(this, oldTrack)
          visibility = View.GONE
        }
      } ?: run {
        hasVideo[index] = track
        binding.surfaceView.apply {
          SurfaceViewRendererUtil.bind(this, track).let { success ->
            if (success) visibility = View.VISIBLE
          }
        }
        binding.name.visibility = View.VISIBLE
      }

      val nameStr = track.peer.name
      val isScreen = track.isScreen
      binding.apply {
        name.text = nameStr
        nameInitials.text = NameUtils.getInitials(nameStr)
        iconScreenShare.visibility = if (isScreen) View.VISIBLE else View.GONE
        iconAudioOff.visibility = if (track.audio?.isMute != false) View.VISIBLE else View.GONE
      }

      index += 1
    }

    while (bindingIt.hasNext()) {
      val binding = bindingIt.next()
      hasVideo[index]?.let { oldTrack ->
        binding.surfaceView.apply {
          visibility = View.GONE
          SurfaceViewRendererUtil.unbind(this, oldTrack)
        }

      }

      hasVideo[index] = null
      binding.name.visibility = View.GONE
      binding.nameInitials.text = "X"
      binding.iconAudioOff.visibility = View.GONE
      binding.iconScreenShare.visibility = View.GONE

      index += 1
    }
  }

  private fun initViewModels() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
      if (tracks.isNotEmpty()) {
        // Pin a screen if possible else pin user's video
        val toPin = tracks.find { it.isScreen } ?: tracks[0]
        changePinViewVideo(toPin)
      }

      Log.d(TAG, "Updated video-list items: size=${tracks.size}")
    }

    meetingViewModel.speakers.observe(viewLifecycleOwner) { speakers ->
      lru.push(speakers.map { it.peerId }.toTypedArray())
      val tracks = ArrayList<MeetingTrack>()
      lru.getItemsInOrder().forEach { peerId ->
        meetingViewModel.tracks.value?.find { it.peer.peerID == peerId }?.let { track ->
          tracks.add(track)
        }
      }

      if (tracks.size > 0) {
        changePinViewVideo(tracks.removeAt(0))
        updateBottomTrayVideoText(tracks.toTypedArray())
      }

    }
  }
}