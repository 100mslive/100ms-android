package live.hms.app2.ui.meeting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.hms.app2.R
import live.hms.app2.databinding.FragmentMeetingBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.home.HomeActivity
import live.hms.app2.ui.meeting.activespeaker.ActiveSpeakerFragment
import live.hms.app2.ui.meeting.activespeaker.HlsFragment
import live.hms.app2.ui.meeting.audiomode.AudioModeFragment
import live.hms.app2.ui.meeting.chat.ChatViewModel
import live.hms.app2.ui.meeting.commons.VideoGridBaseFragment
import live.hms.app2.ui.meeting.pinnedvideo.PinnedVideoFragment
import live.hms.app2.ui.meeting.videogrid.VideoGridFragment
import live.hms.app2.ui.settings.SettingsMode
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.HMSRemovedFromRoom

val LEAVE_INFORMATION_PERSON = "bundle-leave-information-person"
val LEAVE_INFORMATION_REASON = "bundle-leave-information-reason"
val LEAVE_INFROMATION_WAS_END_ROOM = "bundle-leave-information-end-room"
class MeetingFragment : Fragment() {

  companion object {
    private const val TAG = "MeetingFragment"
  }

  private var binding by viewLifecycle<FragmentMeetingBinding>()
  private lateinit var currentFragment: Fragment

  private lateinit var settings: SettingsStore
  private lateinit var roomDetails: RoomDetails

  private val CAPTURE_PERMISSION_REQUEST_CODE = 1

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  private val chatViewModel: ChatViewModel by activityViewModels{
    ChatViewModelFactory(meetingViewModel.hmsSDK)
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
    settings.unregisterOnSharedPreferenceChangeListener(onSettingsChangeListener)
  }

  var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      // There are no request codes
      val data: Intent? = result.data
      meetingViewModel.startScreenshare(data, object : HMSActionResultListener{
        override fun onError(error: HMSException) {
          // error
        }

        override fun onSuccess() {
          // success
        }
      })
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    meetingViewModel.stopScreenshare(object : HMSActionResultListener{
      override fun onError(error: HMSException) {
        // onError
      }

      override fun onSuccess() {
        // onSuccess
      }

    })
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

      R.id.action_record_meeting, R.id.hls_start -> {

        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToRtmpRecordFragment())
      }

      R.id.action_stop_streaming_and_recording -> meetingViewModel.stopRecording()

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

      R.id.hls_view -> {
        meetingViewModel.switchToHlsViewIfRequired()
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

      R.id.action_share_screen -> {
        val mediaProjectionManager: MediaProjectionManager? = requireContext().getSystemService(
          Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        resultLauncher.launch(mediaProjectionManager?.createScreenCaptureIntent())

      }

      R.id.action_stop_share_screen -> {
        meetingViewModel.stopScreenshare(object : HMSActionResultListener{
          override fun onError(error: HMSException) {
            Toast.makeText(activity, " stop screenshare :: $error.description", Toast.LENGTH_LONG).show()
          }

          override fun onSuccess() {
            //success
          }
        })

      }

      R.id.raise_hand -> {
        meetingViewModel.toggleRaiseHand()
      }

      R.id.change_name -> meetingViewModel.requestNameChange()

      R.id.hls_stop -> meetingViewModel.stopHls()
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

    menu.findItem(R.id.raise_hand).isVisible = true
    menu.findItem(R.id.change_name).isVisible = true

    menu.findItem(R.id.add_rtc_stats_observer).apply {
      setOnMenuItemClickListener {
        meetingViewModel.addRTCStatsObserver()
        true
      }
    }

    menu.findItem(R.id.remove_rtc_stats_observer).apply {
      setOnMenuItemClickListener {
        meetingViewModel.removeRtcStatsObserver()
        true
      }
    }

    menu.findItem(R.id.action_stop_streaming_and_recording).apply {
      isVisible = meetingViewModel.isRecording.value == RecordingState.RECORDING ||
              meetingViewModel.isRecording.value == RecordingState.STREAMING ||
              meetingViewModel.isRecording.value == RecordingState.STREAMING_AND_RECORDING
    }

    menu.findItem(R.id.raise_hand).apply {
      if (meetingViewModel.isHandRaised.value == true) {
        title = getString(R.string.lower_hand)
      } else {
        title = getString(R.string.raise_hand)
      }
    }

    menu.findItem(R.id.action_record_meeting).apply {
      isVisible = true

      // If we're in a transitioning state, we prevent further clicks.
      // Checked or not checked depends on if it's currently recording or not. Checked if recording.
      when (meetingViewModel.isRecording.value) {
        RecordingState.STREAMING -> {
          this.isChecked = true
          this.isEnabled = true
          this.title = "Streaming"
        }
        RecordingState.STREAMING_AND_RECORDING -> {
          this.isChecked = true
          this.isEnabled = true
          this.title = "Rec+Stream"
        }
        RecordingState.RECORDING -> {
          this.isChecked = true
          this.isEnabled = true
          this.title = "Recording"
        }
        RecordingState.NOT_RECORDING_OR_STREAMING -> {
          this.isChecked = false
          this.isEnabled = true
          this.title = "Rec+Stream"
        }
        RecordingState.RECORDING_TRANSITIONING_TO_NOT_RECORDING -> {
          this.isChecked = true
          this.isEnabled = false
          this.title = "Recording"
        }
        RecordingState.NOT_RECORDING_TRANSITION_IN_PROGRESS -> {
          this.isChecked = false
          this.isEnabled = false
          this.title = "Recording"
        }
        else -> {
          this.title = "Recording"
        } // Nothing
      }
    }

    (menu.findItem(R.id.toggle_audio_mode))?.apply {
      fun updateState() {
        title = getString(if (meetingViewModel.getCurrentMediaModeCheckedState())
          R.string.audio_mode_media
        else
          R.string.audio_mode_in_call)
        isChecked = meetingViewModel.getCurrentMediaModeCheckedState()
      }
      updateState()
      setOnMenuItemClickListener {
        meetingViewModel.toggleMediaMode()
        updateState()
        true
      }
    }

    menu.findItem(R.id.end_room).apply {
      isVisible = meetingViewModel.isAllowedToEndMeeting()

      setOnMenuItemClickListener {
        meetingViewModel.endRoom(false)
        true
      }
    }

    menu.findItem(R.id.action_enable_background).apply {
      setOnMenuItemClickListener {
        meetingViewModel.startVirtualBackgroundPlugin(context, object : HMSActionResultListener{
          override fun onError(error: HMSException) {
            // error
          }

          override fun onSuccess() {
            // success
          }
        })
        true
      }
    }

    menu.findItem(R.id.action_disable_background).apply {
      setOnMenuItemClickListener {
        meetingViewModel.stopVirtualBackgroundPlugin(object : HMSActionResultListener{
          override fun onError(error: HMSException) {
            // error
          }

          override fun onSuccess() {
            // success
          }
        })
        true
      }
    }

    menu.findItem(R.id.end_and_lock_room).apply {
      isVisible = meetingViewModel.isAllowedToEndMeeting()

      setOnMenuItemClickListener {
        meetingViewModel.endRoom(true)
        true
      }
    }
    val isAllowedToMuteUnmute =
      meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers()
    var remotePeersAreMute: Boolean? = null
    if (isAllowedToMuteUnmute) {
      remotePeersAreMute = meetingViewModel.areAllRemotePeersMute()
    }

    menu.findItem(R.id.remote_mute_all).apply {
      isVisible =
        meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers() && isAllowedToMuteUnmute

      if (isVisible) {
        val text =
          if (remotePeersAreMute == null) "No peers to mute/unmute" else if (remotePeersAreMute) "Remote Unmute All" else "Remote Mute All"
//        text += " " + if(it.type == HMSTrackType.VIDEO) "Video" else "Audio"
        this.title = text
      }

      setOnMenuItemClickListener {

        if (remotePeersAreMute == null) {
          Toast.makeText(
            requireContext(),
            "No remote peers, or their audio tracks are absent",
            Toast.LENGTH_LONG
          ).show()
        } else {
          // If they exist and have a mute status, reverse it.
          meetingViewModel.remoteMute(!remotePeersAreMute, null)
        }
        true
      }
    }

    menu.findItem(R.id.remote_mute_role).apply {
      // Launch a scroll thing.
      isVisible =
        meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers() && isAllowedToMuteUnmute
      val cancelRoleName = "Cancel"
      setOnMenuItemClickListener {
        val availableRoles = meetingViewModel.getAvailableRoles().map { it.name }
        val rolesToSend = availableRoles.plus(cancelRoleName)
        binding.roleSpinner.root.initAdapters(
          rolesToSend,
          if (remotePeersAreMute == null) "Nothing to change" else if (remotePeersAreMute) "Remote Unmute Role" else "Remote Mute Role",
          object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
              parent: AdapterView<*>?,
              view: View?,
              position: Int,
              id: Long
            ) {
              val stringRole = parent?.adapter?.getItem(position) as String
              if (remotePeersAreMute == null) {
                Toast.makeText(
                  requireContext(),
                  "No remote peers, or their audio tracks are absent",
                  Toast.LENGTH_LONG
                ).show()
              } else {
                if (stringRole != cancelRoleName) {
                  meetingViewModel.remoteMute(!remotePeersAreMute, listOf(stringRole))
                }
              }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
              // Nothing
            }

          })
        binding.roleSpinner.root.performClick()
        true
      }
    }

    menu.findItem(R.id.action_flip_camera).apply {
      val ok = meetingViewModel.meetingViewMode.value != MeetingViewMode.AUDIO_ONLY
      isVisible = ok
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
    meetingViewModel.showAudioMuted.observe(
      viewLifecycleOwner,
      Observer { activity?.invalidateOptionsMenu() })
    meetingViewModel.isRecording.observe(
      viewLifecycleOwner,
      Observer { activity?.invalidateOptionsMenu() })
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

  private fun goToHomePage(details: HMSRemovedFromRoom? = null) {
    Intent(requireContext(), HomeActivity::class.java).apply {
      crashlyticsLog(TAG, "MeetingActivity.finish() -> going to HomeActivity :: $this")
      if(details != null) {
        putExtra(LEAVE_INFORMATION_PERSON, details.peerWhoRemoved?.name ?: "Someone")
        putExtra(LEAVE_INFORMATION_REASON, details.reason)
        putExtra(LEAVE_INFROMATION_WAS_END_ROOM, details.roomWasEnded)
      }
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

    viewLifecycleOwner.lifecycleScope.launch {
      meetingViewModel.events.collect { event ->
        when (event) {
          is MeetingViewModel.Event.RTMPError -> {
            withContext(Dispatchers.Main) {
              Toast.makeText(context, "RTMP error ${event.exception}", Toast.LENGTH_LONG).show()
            }
          }
          is MeetingViewModel.Event.ChangeTrackMuteRequest -> {
            withContext(Dispatchers.Main) {
              val message = if (event.request.track is HMSLocalAudioTrack) {
                "${event.request.requestedBy?.name ?: "A peer"} is asking you to unmute."
              } else {
                "${event.request.requestedBy?.name ?: "A peer"} is asking you to turn on video."
              }

              val builder = AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setTitle(R.string.track_change_request)
                .setCancelable(false)

              builder.setPositiveButton(R.string.turn_on) { dialog, _ ->
                if (event.request.track is HMSLocalAudioTrack) {
                  meetingViewModel.setLocalAudioEnabled(true)
                } else if (event.request.track is HMSLocalVideoTrack) {
                  meetingViewModel.setLocalVideoEnabled(true)
                }
                dialog.dismiss()
              }

              builder.setNegativeButton(R.string.reject) { dialog, _ ->
                dialog.dismiss()
              }

              builder.create().apply { show() }

            }
            return@collect
          }
          MeetingViewModel.Event.OpenChangeNameDialog -> {
            withContext(Dispatchers.Main) {
              ChangeNameDialogFragment().show(childFragmentManager, ChangeNameDialogFragment.TAG)
            }
            return@collect
          }
          null -> {}
          is MeetingViewModel.Event.HlsNotStarted -> Toast.makeText(requireContext(), event.reason, Toast.LENGTH_LONG).show()
          is MeetingViewModel.Event.Hls.HlsError -> Toast.makeText(requireContext(), event.throwable.message, Toast.LENGTH_LONG).show()
          is MeetingViewModel.Event.RecordEvent ->
          {
            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            Log.d("RecordingState", event.message)
          }
          is MeetingViewModel.Event.RtmpEvent -> {
            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            Log.d("RecordingState", event.message)
          }
          is MeetingViewModel.Event.ServerRecordEvent -> {
            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            Log.d("RecordingState", event.message)
          }
          is MeetingViewModel.Event.HlsEvent, is MeetingViewModel.Event.HlsRecordingEvent -> {
            Toast.makeText(requireContext(), (event as MeetingViewModel.Event.MessageEvent).message, Toast.LENGTH_LONG).show()
          }
        }
      }
    }

    meetingViewModel.state.observe(viewLifecycleOwner) { state ->
      Log.v(TAG, "Meeting State: $state")
      isMeetingOngoing = false

      when (state) {

        is MeetingState.NonFatalFailure -> {
          val message = state.exception.message
          Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }

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
            meetingViewModel.setStatetoOngoing() // hack, so that the liveData represents the correct state. Use SingleLiveEvent instead
          }

          builder.setNegativeButton(R.string.reject) { dialog, _ ->
            dialog.dismiss()
            alertDialog = null
            meetingViewModel.setStatetoOngoing() // hack, so that the liveData represents the correct state. Use SingleLiveEvent instead
          }

          alertDialog = builder.create().apply { show() }
        }

        is MeetingState.Reconnecting -> {
          if (settings.showReconnectingProgressBars) {
            updateProgressBarUI(state.heading, state.message)
            showProgressBar()
            if (currentFragment is VideoGridBaseFragment)
              (currentFragment as VideoGridBaseFragment).unbindViews()
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
        is MeetingState.Reconnected -> {
          hideProgressBar()
          if (currentFragment is VideoGridBaseFragment)
            (currentFragment as VideoGridBaseFragment).bindViews()

          isMeetingOngoing = true
        }
        is MeetingState.Disconnecting -> {
          updateProgressBarUI(state.heading, state.message)
          showProgressBar()
        }
        is MeetingState.Disconnected -> {
          cleanup()
          hideProgressBar()

          if (state.goToHome) goToHomePage(state.removedFromRoom)
        }

        is MeetingState.ForceLeave -> {
          meetingViewModel.leaveMeeting(state.details)
        }

      }
    }

    meetingViewModel.isLocalAudioPublishingAllowed.observe(viewLifecycleOwner) { allowed ->
      binding.buttonToggleAudio.visibility = if (allowed) View.VISIBLE else View.GONE

    }

    meetingViewModel.isLocalVideoPublishingAllowed.observe(viewLifecycleOwner) { allowed ->
      binding.buttonToggleVideo.visibility = if (allowed) View.VISIBLE else View.GONE
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

    meetingViewModel.peerLiveDate.observe(viewLifecycleOwner) {
      chatViewModel.peersUpdate()
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
    currentFragment = when (mode) {
      MeetingViewMode.GRID -> VideoGridFragment()
      MeetingViewMode.PINNED -> PinnedVideoFragment()
      MeetingViewMode.ACTIVE_SPEAKER -> ActiveSpeakerFragment()
      MeetingViewMode.AUDIO_ONLY -> AudioModeFragment()
      is MeetingViewMode.HLS -> HlsFragment().apply {
        arguments = bundleOf(
              "hlsStreamUrl" to mode.url
      )
      }

    }

    meetingViewModel.setTitle(mode.titleResId)

    if (mode == MeetingViewMode.AUDIO_ONLY) {
      binding.buttonToggleVideo.visibility = View.GONE
    } else {
      binding.buttonToggleVideo.visibility = View.VISIBLE
    }

    childFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, currentFragment)
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