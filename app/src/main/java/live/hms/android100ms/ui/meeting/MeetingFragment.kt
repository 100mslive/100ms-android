package live.hms.android100ms.ui.meeting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import live.hms.android100ms.R
import live.hms.android100ms.audio.HMSAudioManager
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.home.HomeActivity
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.chat.ChatMessage
import live.hms.android100ms.ui.meeting.chat.ChatViewModel
import live.hms.android100ms.ui.meeting.videogrid.GridVideoFragment
import live.hms.android100ms.util.*
import java.util.*


class MeetingFragment : Fragment() {

  companion object {
    private const val TAG = "MeetingFragment"
  }

  private var binding by viewLifecycle<FragmentMeetingBinding>()

  private lateinit var settings: SettingsStore
  private lateinit var roomDetails: RoomDetails

  private val chatViewModel: ChatViewModel by activityViewModels()

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  private lateinit var audioManager: HMSAudioManager

  private var meetingViewMode = MeetingViewMode.GRID_VIEW

  override fun onResume() {
    super.onResume()
    audioManager.updateAudioDeviceState()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails

    audioManager = HMSAudioManager.create(requireContext())
  }

  override fun onStop() {
    super.onStop()
    stopAudioManager()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_share_link -> {
        val meetingUrl = roomDetails.let {
          "https://${it.env}.100ms.live/?room=${it.roomId}&env=${it.env}&role=Guest"
        }
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, meetingUrl)
          type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
      }

      R.id.action_record_meeting -> {
        Toast.makeText(requireContext(), "Recording Not Supported", Toast.LENGTH_SHORT).show()
      }

      R.id.action_share_screen -> {
        Toast.makeText(requireContext(), "Screen Share Not Supported", Toast.LENGTH_SHORT).show()
      }

      R.id.action_email_logs -> {
        requireContext().startActivity(
          EmailUtils.getCrashLogIntent(requireContext())
        )
      }

      R.id.action_grid_view -> {
        changeMeetingMode(MeetingViewMode.GRID_VIEW)
      }

      R.id.action_pinned_view -> {
        changeMeetingMode(MeetingViewMode.PINNED_VIEW)
      }
    }
    return false
  }

  private fun updateActionVolumeMenuIcon(item: MenuItem) {
    item.apply {
      if (meetingViewModel.isAudioMuted) {
        setIcon(R.drawable.ic_baseline_volume_off_24)
      } else {
        setIcon(R.drawable.ic_baseline_volume_up_24)
      }
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    menu.findItem(R.id.action_volume).apply {
      updateActionVolumeMenuIcon(this)
      setOnMenuItemClickListener {
        meetingViewModel.toggleAudio()
        updateActionVolumeMenuIcon(this)

        true
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViewModel()
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentMeetingBinding.inflate(inflater, container, false)
    settings = SettingsStore(requireContext())

    updateVideoView()
    initButtons()
    initOnBackPress()

    meetingViewModel.startMeeting()
    return binding.root
  }

  private fun goToHomePage() {
    Intent(requireContext(), HomeActivity::class.java).apply {
      Log.v(TAG, "MeetingActivity.finish() -> going to HomeActivity :: $this")
      startActivity(this)
    }

    requireActivity().finish()
  }

  private fun initViewModel() {
    chatViewModel.setSendBroadcastCallback { meetingViewModel.broadcastMessage(it) }

    meetingViewModel.state.observe(viewLifecycleOwner) { state ->
      Log.v(TAG, "Meeting State: $state")
      when (state) {
        is MeetingState.Disconnected -> {
          cleanup()
          hideProgressBar()
          stopAudioManager()

          if (state.showDialog) {
            val positiveButtonText = if (state.goToHome) R.string.leave else R.string.retry

            AlertDialog.Builder(requireContext())
              .setMessage(state.message)
              .setTitle(state.heading)
              .setPositiveButton(positiveButtonText) { dialog, _ ->
                Log.d(TAG, "Leaving meeting due to '${state.heading}' :: ${state.message}")

                if (state.goToHome) {
                  goToHomePage()
                } else {
                  meetingViewModel.startMeeting()
                }

                dialog.dismiss()
              }
              .setCancelable(false)
              .create()
              .show()
          } else if (state.goToHome) {
            goToHomePage()
          }
        }

        is MeetingState.Connecting -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.Joining -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.LoadingMedia -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.PublishingMedia -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.Ongoing -> {
          startAudioManager()
          hideProgressBar()
        }
        is MeetingState.Disconnecting -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
      }
    }

    meetingViewModel.isVideoEnabled.observe(viewLifecycleOwner) { enabled ->
      binding.buttonToggleVideo.apply {
        setIconResource(
          if (enabled) R.drawable.ic_baseline_videocam_24
          else R.drawable.ic_baseline_videocam_off_24
        )
      }
    }

    meetingViewModel.isAudioEnabled.observe(viewLifecycleOwner) { enabled ->
      binding.buttonToggleAudio.apply {
        setIconResource(
          if (enabled) R.drawable.ic_baseline_mic_24
          else R.drawable.ic_baseline_mic_off_24
        )
      }
    }

    meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) { data ->
      chatViewModel.receivedMessage(
        ChatMessage(
          data.peer.uid,
          data.senderName,
          Date(),
          data.msg,
          false
        )
      )
    }
  }

  private fun startAudioManager() {
    crashlyticsLog(TAG, "Starting Audio manager")

    audioManager.start { selectedAudioDevice, availableAudioDevices ->
      crashlyticsLog(
        TAG,
        "onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice"
      )
    }
  }

  private fun stopAudioManager() {
    val devices = audioManager.selectedAudioDevice
    crashlyticsLog(TAG, "Stopping Audio Manager:selectedAudioDevice:${devices}")
    audioManager.stop()
  }


  private fun updateProgressBarUI(heading: String, description: String = "") {
    binding.progressBar.heading.text = heading
    binding.progressBar.description.apply {
      visibility = if (description.isEmpty()) View.GONE else View.VISIBLE
      text = description
    }
  }

  private fun changeMeetingMode(newMode: MeetingViewMode) {
    meetingViewMode = newMode
    updateVideoView()
  }

  private fun updateVideoView() {
    /* val fragment = when (meetingViewMode) {
      MeetingViewMode.GRID_VIEW -> GridVideoFragment()
      MeetingViewMode.PINNED_VIEW -> PinnedVideoFragment()
    } */
    val fragment = GridVideoFragment()

    childFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .addToBackStack(null)
      .commit()
  }

  private fun hideProgressBar() {
    binding.fragmentContainer.visibility = View.VISIBLE
    binding.bottomControls.visibility = View.VISIBLE

    binding.progressBar.root.visibility = View.GONE
  }

  private fun showProgressBar() {
    binding.fragmentContainer.visibility = View.GONE
    binding.bottomControls.visibility = View.GONE

    binding.progressBar.root.visibility = View.VISIBLE
  }

  private fun initButtons() {
    binding.buttonToggleVideo.apply {
      visibility = if (settings.publishVideo) View.VISIBLE else View.GONE
      isEnabled = settings.publishVideo

      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleVideo.onClick()")
        meetingViewModel.toggleUserMic()
      }
    }

    binding.buttonToggleAudio.apply {
      visibility = if (settings.publishAudio) View.VISIBLE else View.GONE
      isEnabled = settings.publishAudio

      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleAudio.onClick()")
        meetingViewModel.toggleUserVideo()
      }
    }

    binding.buttonOpenChat.setOnClickListener {
      findNavController().navigate(
        MeetingFragmentDirections.actionMeetingFragmentToChatBottomSheetFragment(
          roomDetails,
          meetingViewModel.peer.customerUserId
        )
      )
    }

    binding.buttonEndCall.setOnSingleClickListener(350L) { meetingViewModel.leaveMeeting() }

    binding.buttonFlipCamera.apply {
      if (!settings.publishVideo) {
        visibility = View.GONE
        isEnabled = false
      } else {
        visibility = View.VISIBLE
        isEnabled = true
        setOnClickListener {
          meetingViewModel.flipCamera()
        }
      }
    }
  }

  private fun cleanup() {
    // Because the scope of Chat View Model is the entire activity
    // We need to perform a cleanup
    chatViewModel.clearMessages()

    stopAudioManager()
    crashlyticsLog(TAG, "cleanup() done")
  }

  private fun initOnBackPress() {
    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
          meetingViewModel.leaveMeeting()
        }
      })
  }
}