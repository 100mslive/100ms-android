package live.hms.app2.ui.meeting

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import live.hms.app2.R
import live.hms.app2.databinding.FragmentMeetingBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.home.HomeActivity
import live.hms.app2.ui.meeting.activespeaker.ActiveSpeakerFragment
import live.hms.app2.ui.meeting.audiomode.AudioModeFragment
import live.hms.app2.ui.meeting.chat.ChatViewModel
import live.hms.app2.ui.meeting.pinnedvideo.PinnedVideoFragment
import live.hms.app2.ui.meeting.videogrid.VideoGridFragment
import live.hms.app2.ui.settings.SettingsMode
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*

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

  private var alertDialog: AlertDialog? = null

  private var isMeetingOngoing = false

  private val onSettingsChangeListener =
    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
      if (SettingsStore.APPLY_CONSTRAINTS_KEYS.contains(key)) {
        // meetingViewModel.updateLocalMediaStreamConstraints()
      }
    }

  override fun onResume() {
    super.onResume()
    settings.registerOnSharedPreferenceChangeListener(onSettingsChangeListener)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    settings = SettingsStore(requireContext())
    roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
  }

  override fun onStop() {
    super.onStop()
    chatViewModel.removeSendBroadcastCallback()
    settings.unregisterOnSharedPreferenceChangeListener(onSettingsChangeListener)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_share_link -> {
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, roomDetails.url)
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
          EmailUtils.getNonFatalLogIntent(requireContext())
        )
      }

      R.id.action_grid_view -> {
        meetingViewModel.setMeetingViewMode(MeetingViewMode.GRID)
      }

      R.id.action_pinned_view -> {
        meetingViewModel.setMeetingViewMode(MeetingViewMode.PINNED)
      }

      R.id.active_speaker_view -> {
        meetingViewModel.setMeetingViewMode(MeetingViewMode.ACTIVE_SPEAKER)
      }

      R.id.audio_only_view -> {
        meetingViewModel.setMeetingViewMode(MeetingViewMode.AUDIO_ONLY)
      }


      R.id.action_settings -> {
        findNavController().navigate(
          MeetingFragmentDirections.actionMeetingFragmentToSettingsFragment(SettingsMode.MEETING)
        )
      }

      R.id.action_participants -> {
        findNavController().navigate(
          MeetingFragmentDirections.actionMeetingFragmentToParticipantsFragment()
        )
      }
    }
    return false
  }

  private fun updateActionVolumeMenuIcon(item: MenuItem) {
    item.apply {
      if (meetingViewModel.isPeerAudioEnabled()) {
        setIcon(R.drawable.ic_volume_up_24)
      } else {
        setIcon(R.drawable.ic_volume_off_24)
      }
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    menu.findItem(R.id.action_flip_camera).apply {
      val ok = meetingViewModel.meetingViewMode.value != MeetingViewMode.AUDIO_ONLY
      setVisible(ok)
    }

    menu.findItem(R.id.action_volume).apply {
      updateActionVolumeMenuIcon(this)
      setOnMenuItemClickListener {
        if (isMeetingOngoing) {
          meetingViewModel.toggleAudio()
          updateActionVolumeMenuIcon(this)
        }

        true
      }
    }

    menu.findItem(R.id.action_flip_camera).apply {
      setOnMenuItemClickListener {
        if (isMeetingOngoing) {
          meetingViewModel.flipCamera()
        }
        true
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViewModel()
    setHasOptionsMenu(true)
    meetingViewModel.showAudioMuted.observe(viewLifecycleOwner, Observer { activity?.invalidateOptionsMenu() })
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentMeetingBinding.inflate(inflater, container, false)

    initButtons()
    initOnBackPress()

    if (meetingViewModel.state.value is MeetingState.Disconnected) {
      // Handles configuration changes
      meetingViewModel.startMeeting()
    }
    return binding.root
  }

  private fun goToHomePage() {
    Intent(requireContext(), HomeActivity::class.java).apply {
      crashlyticsLog(TAG, "MeetingActivity.finish() -> going to HomeActivity :: $this")
      startActivity(this)
    }
    requireActivity().finish()
  }

  private fun initViewModel() {
    meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
      chatViewModel.receivedMessage(it)
    }

    meetingViewModel.meetingViewMode.observe(viewLifecycleOwner) {
      updateVideoView(it)
      requireActivity().invalidateOptionsMenu()
    }

    chatViewModel.setSendBroadcastCallback { meetingViewModel.sendChatMessage(it) }

    chatViewModel.unreadMessagesCount.observe(viewLifecycleOwner) { count ->
      if (count > 0) {
        binding.unreadMessageCount.apply {
          visibility = View.VISIBLE
          text = count.toString()
        }
      } else {
        binding.unreadMessageCount.visibility = View.GONE
      }
    }

    meetingViewModel.state.observe(viewLifecycleOwner) { state ->
      Log.v(TAG, "Meeting State: $state")
      isMeetingOngoing = false

      when (state) {
        is MeetingState.Failure -> {
          alertDialog?.dismiss()
          alertDialog = null

          cleanup()
          hideProgressBar()

          val builder = AlertDialog.Builder(requireContext())
            .setMessage("${state.exceptions.size} failures: \n" + state.exceptions.joinToString("\n\n") { "$it" })
            .setTitle(R.string.error)
            .setCancelable(false)

          builder.setPositiveButton(R.string.retry) { dialog, _ ->
            meetingViewModel.startMeeting()
            dialog.dismiss()
            alertDialog = null
          }

          builder.setNegativeButton(R.string.leave) { dialog, _ ->
            meetingViewModel.leaveMeeting()
            goToHomePage()
            dialog.dismiss()
            alertDialog = null
          }

          builder.setNeutralButton(R.string.bug_report) { _, _ ->
            requireContext().startActivity(
              EmailUtils.getNonFatalLogIntent(requireContext())
            )
            alertDialog = null
          }

          alertDialog = builder.create().apply { show() }
        }

        is MeetingState.RoleChangeRequest -> {
          alertDialog?.dismiss()
          alertDialog = null
          hideProgressBar()

          val builder = AlertDialog.Builder(requireContext())
            .setMessage("${state.hmsRoleChangeRequest.requestedBy?.name} wants to change your role to : \n" + state.hmsRoleChangeRequest.suggestedRole.name)
            .setTitle(R.string.change_role_request)
            .setCancelable(false)

          builder.setPositiveButton(R.string.accept) { dialog, _ ->
            meetingViewModel.changeRoleAccept(state.hmsRoleChangeRequest)
            dialog.dismiss()
            alertDialog = null
          }

          builder.setNegativeButton(R.string.reject) { dialog, _ ->
            dialog.dismiss()
            alertDialog = null
          }

          alertDialog = builder.create().apply { show() }
        }

        is MeetingState.Reconnecting -> {
          if (settings.showReconnectingProgressBars) {
            updateProgressBarUI(state.heading, state.message)
            showProgressBar()
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
          hideProgressBar()

          isMeetingOngoing = true
        }
        is MeetingState.Disconnecting -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.Disconnected -> {
          cleanup()
          hideProgressBar()

          if (state.goToHome) goToHomePage()
        }
      }
    }

    meetingViewModel.isLocalVideoEnabled.observe(viewLifecycleOwner) { enabled ->
      binding.buttonToggleVideo.apply {
        setIconResource(
          if (enabled) R.drawable.ic_videocam_24
          else R.drawable.ic_videocam_off_24
        )
      }
    }

    meetingViewModel.isLocalAudioEnabled.observe(viewLifecycleOwner) { enabled ->
      binding.buttonToggleAudio.apply {
        setIconResource(
          if (enabled) R.drawable.ic_mic_24
          else R.drawable.ic_mic_off_24
        )
      }
    }
  }


  private fun updateProgressBarUI(heading: String, description: String = "") {
    binding.progressBar.heading.text = heading
    binding.progressBar.description.apply {
      visibility = if (description.isEmpty()) View.GONE else View.VISIBLE
      text = description
    }
  }

  private fun updateVideoView(mode: MeetingViewMode) {
    val fragment = when (mode) {
      MeetingViewMode.GRID -> VideoGridFragment()
      MeetingViewMode.PINNED -> PinnedVideoFragment()
      MeetingViewMode.ACTIVE_SPEAKER -> ActiveSpeakerFragment()
      MeetingViewMode.AUDIO_ONLY -> AudioModeFragment()
    }

    meetingViewModel.setTitle(mode.titleResId)

    if (mode == MeetingViewMode.AUDIO_ONLY) {
      binding.buttonToggleVideo.visibility = View.GONE
    } else {
      binding.buttonToggleVideo.visibility = View.VISIBLE
    }

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
      // visibility = View.GONE
      isEnabled = settings.publishVideo

      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleVideo.onClick()")
        meetingViewModel.toggleLocalVideo()
      }
    }

    binding.buttonToggleAudio.apply {
      visibility = if (settings.publishAudio) View.VISIBLE else View.GONE
      // visibility = View.GONE
      isEnabled = settings.publishAudio

      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleAudio.onClick()")
        meetingViewModel.toggleLocalAudio()
      }
    }

    binding.buttonOpenChat.setOnSingleClickListener(1000L) {
      Log.d(TAG, "initButtons: Chat Button clicked")
      findNavController().navigate(
        MeetingFragmentDirections.actionMeetingFragmentToChatBottomSheetFragment(
          roomDetails,
          "Dummy Customer Id"
        )
      )
    }

    binding.buttonEndCall.setOnSingleClickListener(350L) { meetingViewModel.leaveMeeting() }
  }

  private fun cleanup() {
    // Because the scope of Chat View Model is the entire activity
    // We need to perform a cleanup
    chatViewModel.clearMessages()

    crashlyticsLog(TAG, "cleanup() done")
  }

  private fun initOnBackPress() {
    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
          AlertDialog.Builder(requireContext())
            .setMessage("You're about to quit the meeting, are you sure?")
            .setTitle(R.string.leave)
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
              dialog.dismiss()
              meetingViewModel.leaveMeeting()
            }.setNegativeButton("No") { dialog, _ ->
              dialog.dismiss()
            }.create()
            .show()
        }
      })
  }
}