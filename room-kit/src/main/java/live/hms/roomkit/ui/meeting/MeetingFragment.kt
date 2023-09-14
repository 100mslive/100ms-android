package live.hms.roomkit.ui.meeting

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.setGradient
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.activespeaker.ActiveSpeakerFragment
import live.hms.roomkit.ui.meeting.activespeaker.HlsFragment
import live.hms.roomkit.ui.meeting.audiomode.AudioModeFragment
import live.hms.roomkit.ui.meeting.bottomsheets.LeaveCallBottomSheet
import live.hms.roomkit.ui.meeting.bottomsheets.MultipleLeaveOptionBottomSheet
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipBroadcastReceiver
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils.disconnectCallPipEvent
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils.muteTogglePipEvent
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.meeting.chat.combined.ChatParticipantCombinedFragment
import live.hms.roomkit.ui.meeting.chat.combined.OPEN_TO_CHAT_ALONE
import live.hms.roomkit.ui.meeting.chat.combined.OPEN_TO_PARTICIPANTS
import live.hms.roomkit.ui.meeting.commons.VideoGridBaseFragment
import live.hms.roomkit.ui.meeting.participants.ParticipantsFragment
import live.hms.roomkit.ui.meeting.participants.RtmpRecordBottomSheet
import live.hms.roomkit.ui.meeting.pinnedvideo.PinnedVideoFragment
import live.hms.roomkit.ui.meeting.videogrid.VideoGridFragment
import live.hms.roomkit.ui.notification.HMSNotificationType
import live.hms.roomkit.ui.settings.SettingsMode
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.*
import live.hms.roomkit.util.*
import live.hms.video.audio.HMSAudioManager
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.HMSHlsRecordingConfig
import live.hms.video.sdk.models.HMSRemovedFromRoom


val LEAVE_INFORMATION_PERSON = "bundle-leave-information-person"
val LEAVE_INFORMATION_REASON = "bundle-leave-information-reason"
val LEAVE_INFROMATION_WAS_END_ROOM = "bundle-leave-information-end-room"

class MeetingFragment : Fragment() {
    private val chatAdapter = ChatAdapter()
    companion object {
        private const val TAG = "MeetingFragment"
        const val AudioSwitchBottomSheetTAG = "audioSwitchBottomSheet"
    }

    private var startPollSnackBar : Snackbar? = null
    private var binding by viewLifecycle<FragmentMeetingBinding>()
    private lateinit var currentFragment: Fragment




    private lateinit var settings: SettingsStore
    var countDownTimer: CountDownTimer? = null
    var isCountdownManuallyCancelled: Boolean = false

    private val args: MeetingFragmentArgs by navArgs()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    private val chatViewModel: ChatViewModel by activityViewModels {
        ChatViewModelFactory(meetingViewModel.hmsSDK)
    }


    private var isMeetingOngoing = false

    private val rtmpBottomSheet by lazy {
        RtmpRecordBottomSheet {
//            binding.buttonGoLive?.visibility = View.GONE
        }
    }

    private val onSettingsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (SettingsStore.APPLY_CONSTRAINTS_KEYS.contains(key)) {
                // meetingViewModel.updateLocalMediaStreamConstraints()
            }
        }

    override fun onResume() {
        super.onResume()
        val poll = meetingViewModel.hasPoll()
        if(poll != null) {
            showPollStart(poll.pollId)
        }
        isCountdownManuallyCancelled = false
        setupRecordingTimeView()
        settings.registerOnSharedPreferenceChangeListener(onSettingsChangeListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsStore(requireContext())
    }

    override fun onStop() {
        super.onStop()
        settings.unregisterOnSharedPreferenceChangeListener(onSettingsChangeListener)
        cancelCallback()
    }

    private fun cancelCallback() = handler.removeCallbacks(hideRunnable)

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                meetingViewModel.startScreenshare(data, object : HMSActionResultListener {
                    override fun onError(error: HMSException) {
                        // error
                    }

                    override fun onSuccess() {
                        // success
                        meetingViewModel.isScreenShare.postValue(true)
                    }
                })
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        isCountdownManuallyCancelled = true
        countDownTimer?.cancel()
        unregisterPipActionListener()
    }

    override fun onPause() {
        super.onPause()
        isCountdownManuallyCancelled = true
        countDownTimer?.cancel()
    }

    override fun onStart() {
        super.onStart()
        isCountdownManuallyCancelled = false
        setupRecordingTimeView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share_link -> {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }

            R.id.sessionMetadataAlpha -> {
                findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToRoomMetadataAlphaFragment())
            }

            R.id.action_record_meeting, R.id.hls_start -> {

//                findNavController().navigate(
//                    MeetingFragmentDirections.actionMeetingFragmentToRtmpRecordFragment(
//                        roomDetails.url
//                    )
//                )
            }

            R.id.action_stop_streaming_and_recording -> meetingViewModel.stopRecording()

            R.id.action_email_logs -> {
                requireContext().startActivity(
                    EmailUtils.getNonFatalLogIntent(requireContext())
                )
            }
            R.id.action_stats -> {
                val deviceStatsBottomSheet = DeviceStatsBottomSheet()
                deviceStatsBottomSheet.show(requireActivity().supportFragmentManager,"deviceStatsBottomSheet")
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

            R.id.action_bulk_role_change -> {
                findNavController().navigate(
                    MeetingFragmentDirections.actionMeetingFragmentToRoleChangeFragment()
                )
            }

            R.id.action_participants -> {
                // Possibly unused
                val directions = if(meetingViewModel.prebuiltInfoContainer.isChatOverlay()) {
                    MeetingFragmentDirections.actionMeetingFragmentToParticipantsFragment()
                } else {
                    MeetingFragmentDirections.actionMeetingFragmentToParticipantsTabFragment()

                }
                findNavController().navigate(directions)
            }

            R.id.action_share_screen -> {
                val mediaProjectionManager: MediaProjectionManager? =
                    requireContext().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE
                    ) as MediaProjectionManager
                resultLauncher.launch(mediaProjectionManager?.createScreenCaptureIntent())

            }

            R.id.action_stop_share_screen -> {
            }

            R.id.pip_mode -> {
                launchPipMode()
            }

            R.id.raise_hand -> {
                meetingViewModel.toggleRaiseHand()
            }


            R.id.change_name -> meetingViewModel.requestNameChange()

            R.id.hls_stop -> meetingViewModel.stopHls()
        }
        return false
    }

    private fun updateActionVolumeMenuIcon(
        audioOutputType: HMSAudioManager.AudioDevice? = null
    ) {
        binding.iconOutputDevice?.visibility = View.VISIBLE
        binding.iconOutputDevice?.apply {
            when (audioOutputType) {
                HMSAudioManager.AudioDevice.EARPIECE -> {
                    setIconEnabled(R.drawable.phone)
                }

                HMSAudioManager.AudioDevice.SPEAKER_PHONE -> {
                    setIconEnabled(R.drawable.ic_icon_speaker)
                }

                HMSAudioManager.AudioDevice.AUTOMATIC -> {
                    setIconEnabled(R.drawable.ic_icon_speaker)
                }

                HMSAudioManager.AudioDevice.BLUETOOTH -> {
                    setIconEnabled(R.drawable.bt)
                }

                HMSAudioManager.AudioDevice.WIRED_HEADSET -> {
                    setIconEnabled(R.drawable.wired)
                }

                else -> {
                    setIconEnabled(R.drawable.ic_volume_off_24)
                }
            }
        }
    }

    private fun updateGoLiveButton(recordingState: RecordingState) {
        if ((meetingViewModel.isHlsKitUrl || meetingViewModel.hmsSDK.getLocalPeer()
                ?.isWebrtcPeer() == true) && (meetingViewModel.isAllowedToHlsStream() || meetingViewModel.isAllowedToRtmpStream())
        ) {
//            binding.buttonGoLive?.visibility = View.VISIBLE
        } else {
//            binding.buttonGoLive?.visibility = View.GONE
        }
        if (recordingState == RecordingState.STREAMING_AND_RECORDING ) {
            binding.meetingFragmentProgress?.visibility = View.GONE
//            binding.buttonGoLive?.setImageDrawable(
//                ContextCompat.getDrawable(
//                    requireContext(),
//                    R.drawable.ic_stop_circle
//                )
//            )
//            binding.buttonGoLive?.setBackgroundAndColor(DefaultTheme.getColours()?.alertErrorDefault,
//                DefaultTheme.getDefaults().error_default)
            binding.liveTitleCard?.visibility = View.VISIBLE
            binding.tvViewersCountCard?.visibility = View.VISIBLE
            binding.recordingSignal.visibility = View.VISIBLE

            if (meetingViewModel.isRTMPRunning()) {
                binding.liveTitle?.text = "Live with RTMP"
            } else {
                binding.liveTitle?.text = "Live"
            }
            binding.tvViewersCount?.visibility = View.VISIBLE
            binding.tvViewersCountCard.visibility = View.VISIBLE
            setupRecordingTimeView()
        } else if (recordingState == RecordingState.RECORDING ){
            binding.liveTitleCard?.visibility = View.GONE
            binding.recordingSignal.visibility = View.VISIBLE
            binding.tvViewersCount?.visibility = View.GONE
            binding.tvViewersCountCard.visibility = View.GONE
        } else if (recordingState == RecordingState.STREAMING) {
            binding.liveTitleCard?.visibility = View.VISIBLE
            binding.tvViewersCountCard?.visibility = View.VISIBLE
            binding.recordingSignal.visibility = View.GONE

            if (meetingViewModel.isRTMPRunning()) {
                binding.liveTitle?.text = "Live with RTMP"
            } else {
                binding.liveTitle?.text = "Live"
            }
            binding.tvViewersCount?.visibility = View.VISIBLE
            binding.tvViewersCountCard.visibility = View.VISIBLE
        }
        else {
            binding.recordingSignal.visibility = View.GONE
            binding.liveTitleCard?.visibility = View.GONE
            binding.tvViewersCount?.visibility = View.GONE
            binding.tvViewersCountCard.visibility = View.GONE
        }
    }

    private fun setupRecordingTimeView() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(1000, 1000) {
            override fun onTick(l: Long) {
                val startedAt =
                   meetingViewModel.hmsSDK.getRoom()?.hlsStreamingState?.variants?.firstOrNull()?.startedAt
                        ?: meetingViewModel.hmsSDK.getRoom()?.rtmpHMSRtmpStreamingState?.startedAt
                startedAt?.let {
                    if (startedAt > 0) {
                        binding.tvRecordingTime?.visibility = View.VISIBLE
                        binding.tvRecordingTime?.text =
                            millisecondsToTime(System.currentTimeMillis().minus(startedAt))
                    }
                }
            }

            override fun onFinish() {
                //Code hear
                if (isCountdownManuallyCancelled) {
                    return
                }
                start()
            }
        }.start()
    }

    private fun millisecondsToTime(milliseconds: Long): String? {
        val minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60
        val secondsStr = seconds.toString()
        val secs: String = if (secondsStr.length >= 2) {
            secondsStr.substring(0, 2)
        } else {
            "0$secondsStr"
        }
        return "$minutes:$secs"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        initObservers()
        setHasOptionsMenu(true)
        meetingViewModel.showAudioMuted.observe(
            viewLifecycleOwner,
            Observer { activity?.invalidateOptionsMenu() })
        meetingViewModel.isRecording.observe(
            viewLifecycleOwner,
            Observer {
                updateGoLiveButton(it)
            })


        meetingViewModel.isHandRaised.observe(viewLifecycleOwner) { isHandRaised ->
            if (isHandRaised) {
                binding.buttonRaiseHand?.setIconDisabled(R.drawable.ic_raise_hand)
            } else {
                binding.buttonRaiseHand?.setIconEnabled(R.drawable.ic_raise_hand)
            }
        }
        binding.iconSend.setOnSingleClickListener {
            val messageStr = binding.editTextMessage.text.toString().trim()
            if (messageStr.isNotEmpty()) {
                chatViewModel.sendMessage(messageStr)
                binding.editTextMessage.setText("")
            }
        }
        ChatUseCase().initiate(chatViewModel.messages, viewLifecycleOwner, chatAdapter, binding.chatMessages, chatViewModel, null) {
            meetingViewModel.prebuiltInfoContainer.isChatEnabled()
        }
        if(meetingViewModel.prebuiltInfoContainer.chatInitialStateOpen()) {
            binding.buttonOpenChat.setIconDisabled(R.drawable.ic_chat_message)
        } else {
            binding.buttonOpenChat.setIconEnabled(R.drawable.ic_chat_message)
        }
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
        } else {
            //start HLS stream
           if (args.startHlsStream && meetingViewModel.isAllowedToHlsStream()) {
               binding.meetingFragmentProgress?.visibility = View.VISIBLE
               meetingViewModel.startHls(settings.lastUsedMeetingUrl, HMSHlsRecordingConfig(true, false))
           }

        }
        return binding.root
    }

    private fun goToHomePage(details: HMSRemovedFromRoom? = null) {

        //only way to programmatically dismiss pip mode
        if (activity?.isInPictureInPictureMode == true) {
            activity?.moveTaskToBack(false)
        }

        /*Intent(requireContext(), HomeActivity::class.java).apply {
            crashlyticsLog(TAG, "MeetingActivity.finish() -> going to HomeActivity :: $this")
            if (details != null) {
                putExtra(LEAVE_INFORMATION_PERSON, details.peerWhoRemoved?.name ?: "Someone")
                putExtra(LEAVE_INFORMATION_REASON, details.reason)
                putExtra(LEAVE_INFROMATION_WAS_END_ROOM, details.roomWasEnded)
            }
            startActivity(this)
        }*/
        requireActivity().finish()
    }

    private fun initObservers() {

        meetingViewModel.showHlsStreamYetToStartError.observe(viewLifecycleOwner) { showError ->
                binding.streamYetToStartContainer?.visibility = if (showError) View.VISIBLE else View.GONE
        }

        meetingViewModel.peerCount.observe(viewLifecycleOwner) {
            binding.tvViewersCount?.text =it.toString()

        }

        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }

        meetingViewModel.isRecordingInProgess.observe(viewLifecycleOwner) {
            if (it) {
                binding.recordingSignalProgress.visibility = View.VISIBLE
            } else {
                binding.recordingSignalProgress.visibility = View.GONE
            }
        }


        meetingViewModel.isRecording.observe(viewLifecycleOwner) {recordingState ->
            val isRecording = meetingViewModel.isRecordingState()
            binding.recordingSignal.visibility =  if (isRecording) View.VISIBLE else View.GONE
        }


        meetingViewModel.isScreenShare.observe(viewLifecycleOwner) {
            meetingViewModel.triggerScreenShareNotification(it)
        }

        meetingViewModel.hlsToggleUpdateLiveData.observe(viewLifecycleOwner) {
            when(it) {
                true -> binding.meetingFragmentProgress?.visibility = View.VISIBLE
                false -> binding.meetingFragmentProgress?.visibility = View.GONE
            }
        }

        meetingViewModel.meetingViewMode.observe(viewLifecycleOwner) {
            updateMeetingViewMode(it)
            Log.d(TAG, "Meeting view mode changed to $it")
            requireActivity().invalidateOptionsMenu()
        }

        chatViewModel.unreadMessagesCount.observe(viewLifecycleOwner) { count ->
            if(meetingViewModel.prebuiltInfoContainer.isChatEnabled()) {
                if (count > 0) {
                    binding.unreadMessageCount.apply {
                        visibility = View.VISIBLE
                        text = count.toString()
                    }
                } else {
                    binding.unreadMessageCount.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            meetingViewModel.events.collect { event ->
                when (event) {
                    is MeetingViewModel.Event.SessionMetadataEvent -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    is MeetingViewModel.Event.CameraSwitchEvent -> {
                       //Silently ignore
                    }
                    is MeetingViewModel.Event.RTMPError -> {
                        meetingViewModel.triggerErrorNotification("RTMP error ${event.exception}")
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
                            ChangeNameDialogFragment().show(
                                childFragmentManager,
                                ChangeNameDialogFragment.TAG
                            )
                        }
                        return@collect
                    }
                    null -> {
                    }
                    is MeetingViewModel.Event.HlsNotStarted -> meetingViewModel.triggerErrorNotification(event.reason)
                    is MeetingViewModel.Event.Hls.HlsError -> meetingViewModel.triggerErrorNotification(event.throwable.message)
                    is MeetingViewModel.Event.RecordEvent -> {
//                        meetingViewModel.triggerErrorNotification(event.message)
                        Log.d("RecordingState", event.message)
                    }
                    is MeetingViewModel.Event.RtmpEvent -> {
                        meetingViewModel.triggerErrorNotification(event.message)
                        Log.d("RecordingState", event.message)
                    }
                    is MeetingViewModel.Event.ServerRecordEvent -> {
                        meetingViewModel.triggerErrorNotification(event.message)
                        Log.d("RecordingState", event.message)
                    }
                    is MeetingViewModel.Event.HlsEvent, is MeetingViewModel.Event.HlsRecordingEvent -> {
                        Log.d("RecordingState", "HlsEvent: ${event}")
                    }
                    is MeetingViewModel.Event.PollStarted -> {
                        showPollStart(event.hmsPoll.pollId)
                    }

                    else -> null
                }
            }
        }

        meetingViewModel.state.observe(viewLifecycleOwner) { state ->
            Log.v(TAG, "Meeting State: $state")
            isMeetingOngoing = false

            when (state) {

                is MeetingState.NonFatalFailure -> {
                    val message = state.exception.message
                    meetingViewModel.triggerErrorNotification(message)
                }

                is MeetingState.Failure -> {
                    cleanup()
                    hideProgressBar()
                    meetingViewModel.triggerErrorNotification("${state.exceptions.size} failures: \n" + state.exceptions.joinToString(
                        "\n\n"
                    ) { "$it" },
                        isDismissible = false,
                        actionButtonText = resources.getString(R.string.retry),
                        type = HMSNotificationType.TerminalError
                    )
                }

                is MeetingState.RoleChangeRequest -> {
                    //TODO remove from nav graph
                    findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToRolePreviewFragment())
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
                    val message = with(state.details) {
                        if (roomWasEnded) {
                            "Room ended by ${peerWhoRemoved?.name}"
                        } else {
                            "${peerWhoRemoved?.name} removed you from the room. ${state.details.reason}"
                        }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                    meetingViewModel.leaveMeeting(state.details)
                }

            }
        }

        meetingViewModel.isLocalAudioPresent.observe(viewLifecycleOwner) { allowed ->
            binding.buttonToggleAudio.visibility = if (allowed) View.VISIBLE else View.GONE
            //to show or hide mic icon [eg in HLS mode mic is not required]
            updatePipMicState(allowed, true)
        }

        meetingViewModel.isLocalVideoPresent.observe(viewLifecycleOwner) { allowed ->
            binding.buttonToggleVideo.visibility = if (allowed) View.VISIBLE else View.GONE
            binding.buttonSwitchCamera?.visibility = if (allowed) View.VISIBLE else View.GONE
        }

        meetingViewModel.isLocalVideoEnabled.observe(viewLifecycleOwner) { enabled ->
            (binding.buttonToggleVideo)?.apply {
                if (enabled) {
                    setIconEnabled(R.drawable.avd_video_off_to_on)
                    binding.buttonSwitchCamera?.alpha = 1.0f
                    binding.buttonSwitchCamera?.isEnabled = true
                } else {
                    setIconDisabled(R.drawable.avd_video_on_to_off)
                    binding.buttonSwitchCamera?.alpha = 0.5f
                    binding.buttonSwitchCamera?.isEnabled = false
                }
            }
        }

        meetingViewModel.isLocalAudioEnabled.observe(viewLifecycleOwner) { enabled ->
            //enable/disable mic on/off state
            updatePipMicState(isMicOn = enabled)
            (binding.buttonToggleAudio as? ShapeableImageView)?.apply {

                if (enabled) {
                    setIconEnabled(R.drawable.avd_mic_off_to_on)
                } else {
                    setIconDisabled(R.drawable.avd_mic_on_to_off)
                }
            }
        }

        meetingViewModel.peerLiveData.observe(viewLifecycleOwner) {
            chatViewModel.peersUpdate()
        }

        meetingViewModel.roleChange.observe(viewLifecycleOwner) {
            updateChatButtonWhenRoleChanges()
        }
    }


    private fun showPollStart(pollId: String) {
        startPollSnackBar?.dismiss()
        startPollSnackBar = Snackbar.make(binding.root, "View Poll", Snackbar.LENGTH_INDEFINITE).setAction("Open") {
            findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToPollDisplayFragment(pollId))}

        startPollSnackBar?.show()
    }

    private val pipReceiver by lazy {
        PipBroadcastReceiver(
            toogleLocalAudio = meetingViewModel::toggleLocalAudio,
            disconnectCall = meetingViewModel::leaveMeeting
        )
    }


    private fun registerPipActionListener() {
        activity?.let { pipReceiver.register(it) }
    }

    private fun unregisterPipActionListener() {
        activity?.let { pipReceiver.unregister(it) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        registerPipActionListener()
    }

    private fun updatePipEndCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            pipActionsMap[disconnectCallPipEvent] = RemoteAction(
                Icon.createWithResource(activity, R.drawable.ic_call_end_24),
                "End call",
                "",
                PipUtils.getEndCallBroadcast(requireActivity())
            )
            updatePipActions()
        }
    }

    private fun updatePipMicState(isMicShown: Boolean = true, isMicOn: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            if (isMicShown) {
                pipActionsMap[muteTogglePipEvent] = RemoteAction(
                    Icon.createWithResource(
                        activity, if (isMicOn) R.drawable.ic_mic_24
                        else R.drawable.ic_mic_off_24
                    ),
                    "Toggle Audio",
                    "",
                    PipUtils.getToggleMuteBroadcast(requireActivity())
                )
            } else {
                pipActionsMap.remove(muteTogglePipEvent)
            }
        }
        updatePipActions()
    }


    private fun updateProgressBarUI(heading: String, description: String = "") {
//        binding.progressBar.heading.text = heading
//        binding.progressBar.description.apply {
//            visibility = if (description.isEmpty()) View.GONE else View.VISIBLE
//            text = description
//        }
    }

    private val handler by lazy { Handler(Looper.myLooper()!!) }
    private val hideRunnable = Runnable { hideControlBars() }

    private fun updateMeetingViewMode(mode: MeetingViewMode) {

        val modeEnteredOrExitedHls = !this::currentFragment.isInitialized || (currentFragment is HlsFragment && mode !is MeetingViewMode.HLS_VIEWER
                || currentFragment !is HlsFragment && mode is MeetingViewMode.HLS_VIEWER)
        val triggerFirstUpdate = !this::currentFragment.isInitialized

        currentFragment = when (mode) {
            MeetingViewMode.GRID -> VideoGridFragment()
            MeetingViewMode.PINNED -> PinnedVideoFragment()
            MeetingViewMode.ACTIVE_SPEAKER -> ActiveSpeakerFragment()
            MeetingViewMode.AUDIO_ONLY -> AudioModeFragment()
            is MeetingViewMode.HLS_VIEWER -> HlsFragment().apply {
                arguments = bundleOf(
                    "hlsStreamUrl" to mode.url
                )
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, currentFragment)
            .addToBackStack(null)
            .commit()

        if(modeEnteredOrExitedHls) {
            val overlayIsVisible = isOverlayChatVisible()
            if (meetingViewModel.prebuiltInfoContainer.isChatEnabled()) {
                val isChatOverlay = meetingViewModel.prebuiltInfoContainer.isChatOverlay()
                if (overlayIsVisible && !isChatOverlay)
                    toggleChatVisibility()
                else if (!overlayIsVisible && isChatOverlay)
                    toggleChatVisibility()
            } else if (overlayIsVisible) {
                toggleChatVisibility()
            }
        }
        if(triggerFirstUpdate){
            updateChatButtonWhenRoleChanges()
        }
        setupConfiguration(mode)
    }

    private fun updateChatButtonWhenRoleChanges() {
        if(meetingViewModel.prebuiltInfoContainer.isChatEnabled()) {
            binding.messageMenu.visibility = View.VISIBLE
        } else {
            binding.messageMenu.visibility = View.GONE
        }
        if(meetingViewModel.prebuiltInfoContainer.chatInitialStateOpen()) {
            binding.buttonOpenChat.setIconDisabled(R.drawable.ic_chat_message)
        } else {
            binding.buttonOpenChat.setIconEnabled(R.drawable.ic_chat_message)
        }
    }
    var controlBarsVisible = true
    private fun setupConfiguration(mode: MeetingViewMode) {
        if (mode is MeetingViewMode.HLS_VIEWER) {
            configureHLSView()
        } else {
            configureWebrtcView()
        }
    }

    private fun configureWebrtcView() {


        binding.topMenu?.visibility = View.VISIBLE
        binding.bottomControls.visibility  = View.VISIBLE
        showControlBars(false)
        cancelCallback()

        val fragmentContainerParam = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        fragmentContainerParam.addRule(RelativeLayout.BELOW, R.id.top_menu)
        fragmentContainerParam.addRule(RelativeLayout.ABOVE, R.id.bottom_controls)
        binding.fragmentContainer.layoutParams = fragmentContainerParam



        binding.topMenu?.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDim,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )
        binding.bottomControls.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDim,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )
        binding.buttonRaiseHand?.visibility = View.GONE

        WindowCompat.setDecorFitsSystemWindows(activity!!.window, true)

        showSystemBars()
    }

    private fun configureHLSView() {
        updateBindings()


        delayedHide(3000)
    }

    private fun updateBindings() {
        val fragmentContainerParam = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        fragmentContainerParam.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        fragmentContainerParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        binding.fragmentContainer.layoutParams = fragmentContainerParam

        binding.topMenu.setGradient(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
        )
            , Color.TRANSPARENT)


        binding.bottomControls.setGradient(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
        )
        , Color.TRANSPARENT, GradientDrawable.Orientation.BOTTOM_TOP)

        binding.buttonRaiseHand?.visibility = View.VISIBLE

        binding.fragmentContainer.setOnSingleClickListener(500L) {
            if (controlBarsVisible)
                hideControlBars()
            else
                showControlBars(true)
        }
    }


    private fun showSystemBars() {

        activity?.let {
            val windowInsetsController = WindowCompat.getInsetsController(it.window, it.window.decorView)
            windowInsetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun showControlBars(shouldHideAfterDelay : Boolean) {
        controlBarsVisible = true
        binding.topMenu.animate()
            ?.translationY(0f)?.setDuration(300)?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    binding.topMenu.visibility = View.VISIBLE
                    showSystemBars()
                    // This prevents the bar from moving twice as high as it should
                    if(shouldHideAfterDelay)
                        moveChat(up = true, bottomMenuHeight = binding.topMenu.height.toFloat())
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.topMenu.visibility = View.VISIBLE
                    controlBarsVisible = true
                    if (shouldHideAfterDelay) {
                        // Hide control bars
                        delayedHide(3000)
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    binding.topMenu?.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

            })?.start()

        val screenHeight = activity!!.window.decorView.height
        binding.bottomControls.animate()
            ?.translationY(0f)?.setDuration(300)?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    binding.bottomControls.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.bottomControls.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationCancel(animation: Animator?) {
                    binding.bottomControls.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

            })?.start()
    }

    private fun moveChat(up: Boolean, bottomMenuHeight: Float) {
        if(binding.chatView.visibility != View.VISIBLE)
            return

        with(binding.chatView!!){
            if(up) {
                (layoutParams as RelativeLayout.LayoutParams).apply {
                    removeRule(RelativeLayout.ALIGN_BOTTOM)
                    addRule(RelativeLayout.ABOVE, R.id.bottom_controls)
                    updateMargins(bottom = bottomMenuHeight.toInt() + resources.getDimension(R.dimen.eight_dp).toInt())
                }
            } else {
                (layoutParams as RelativeLayout.LayoutParams).apply {
                    removeRule(RelativeLayout.ABOVE)
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.id.meeting_container)
                    updateMargins(bottom = 8)
                }
            }
        }

    }
    private fun hideControlBars() {
        val topMenu = binding.topMenu
        val bottomMenu = binding.bottomControls
        val screenHeight = activity!!.window.decorView.height
        controlBarsVisible = false
        topMenu?.animate()
            ?.translationY(-(topMenu.height.toFloat()))?.setDuration(300)
            ?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    topMenu.visibility = View.VISIBLE
                    moveChat(up = false, topMenu!!.height.toFloat())
                }

                override fun onAnimationEnd(animation: Animator?) {
                    topMenu.visibility = View.GONE
                    controlBarsVisible = false
                }

                override fun onAnimationCancel(animation: Animator?) {
                    topMenu.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

            })?.start()

        bottomMenu.animate()
            ?.translationY((bottomMenu.height.toFloat()))?.setDuration(300)
            ?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    bottomMenu.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {
                    bottomMenu.visibility = View.GONE
                    controlBarsVisible = false
                }

                override fun onAnimationCancel(animation: Animator?) {
                    bottomMenu.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

            })?.start()
    }

    private fun delayedHide(delayMillis: Int) {
        cancelCallback()
        handler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun hideProgressBar() {
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.VISIBLE
        if (activity?.isInPictureInPictureMode?.not() == true && (meetingViewModel.meetingViewMode.value is MeetingViewMode.HLS_VIEWER).not()){
            binding.bottomControls.visibility = View.VISIBLE
        }

        binding.progressBar.root.visibility = View.GONE
        binding.meetingFragmentProgress?.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.VISIBLE

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




        binding.buttonSettingsMenu?.apply {

            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonSettingsMenu.onClick()")
                if (meetingViewModel.isPrebuiltDebugMode().not()){
                    SessionOptionBottomSheet(
                        onScreenShareClicked = { startOrStopScreenShare() },
                        onBRBClicked = { meetingViewModel.toggleBRB() },
                        onPeerListClicked = {
                            if( meetingViewModel.prebuiltInfoContainer.isChatOverlay() ||
                                    !meetingViewModel.prebuiltInfoContainer.isChatEnabled()
                            ) {
                                if(isOverlayChatVisible()){
                                    toggleChatVisibility()
                                }
                                childFragmentManager
                                    .beginTransaction()
                                    .add(R.id.fragment_container, ParticipantsFragment())
                                    .commit()
                            } else {
                                val args = Bundle()
                                    .apply {
                                        putBoolean(OPEN_TO_PARTICIPANTS, true)
                                    }

                                ChatParticipantCombinedFragment()
                                    .apply { arguments = args }
                                    .show(
                                    childFragmentManager,
                                    ChatParticipantCombinedFragment.TAG
                                )
                            }
                        },
                        onRaiseHandClicked = { meetingViewModel.toggleRaiseHand()},
                        onNameChange = {  },
                        onRecordingClicked = {
                            if (meetingViewModel.isRecordingState().not()) {
                                meetingViewModel.recordMeeting(true, runnable = it)
                            } else {
                                StopRecordingBottomSheet {
                                    contextSafe { context, activity ->
                                        meetingViewModel.stopRecording()
                                    }
                                }.show(
                                    childFragmentManager,
                                    StopRecordingBottomSheet.TAG
                                )
                            }


                        },
                    ).show(
                        childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                    )

                } else {
                    val settingsBottomSheet = SettingsBottomSheet(meetingViewModel, {
                        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToParticipantsFragment())
                    }, {
                        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToRoleChangeFragment())
                    },
                        {
                            startPollSnackBar?.dismiss()
                            findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToPollsCreationFragment())
                        })
                    settingsBottomSheet.show(
                        requireActivity().supportFragmentManager,
                        "settingsBottomSheet"
                    )
                }
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

        val chatVisiBility = if(meetingViewModel.prebuiltInfoContainer.isChatEnabled())
            View.VISIBLE
        else
            View.GONE

        binding.buttonOpenChat.visibility = chatVisiBility
        binding.messageMenu.visibility = chatVisiBility
        binding.buttonOpenChat.setOnSingleClickListener {
            if( !meetingViewModel.prebuiltInfoContainer.isChatOverlay()) {
                ChatParticipantCombinedFragment().apply {
                    arguments = Bundle().apply { putBoolean(OPEN_TO_CHAT_ALONE,
                        !meetingViewModel.isParticpantListEnabled()
                    ) }
                }.show(
                    childFragmentManager,
                    ChatParticipantCombinedFragment.TAG
                )
            } else {
                toggleChatVisibility()
            }
        }

        if(meetingViewModel.prebuiltInfoContainer.chatInitialStateOpen()) {
            binding.buttonOpenChat.callOnClick()
        }

        binding.buttonRaiseHand.setOnSingleClickListener(350L) { meetingViewModel.toggleRaiseHand() }

        binding.buttonEndCall.setOnSingleClickListener(350L) { requireActivity().onBackPressed() }

        updatePipEndCall()

        binding.iconOutputDevice?.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "iconOutputDevice.onClick()")

                AudioOutputSwitchBottomSheet { audioDevice, isMuted ->
                    updateActionVolumeMenuIcon(audioDevice)
                }.show(
                    childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                )
            }
        }

        updateActionVolumeMenuIcon(meetingViewModel.getAudioOutputRouteType())

        binding.buttonSwitchCamera?.setOnSingleClickListener(200L) {
            meetingViewModel.flipCamera()
            if (it.isEnabled) meetingViewModel.flipCamera()
        }

        if (meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url.isNullOrEmpty()) {
            binding.logoIv?.visibility = View.GONE
        } else {
            binding.logoIv?.visibility = View.VISIBLE
            binding.logoIv?.let {
                Glide.with(this)
                    .load(meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url)
                    .into(it)
            }
        }
    }

    private fun isOverlayChatVisible() : Boolean {
        return binding.chatView.visibility == View.VISIBLE
    }
    private fun toggleChatVisibility() {
        with(binding.chatView) {
            visibility = if (visibility == View.GONE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        binding.chatMessages.visibility = binding.chatView.visibility
        // Scroll to the latest message if it's visible
        if (binding.chatMessages.visibility == View.VISIBLE) {
            val position = chatAdapter.itemCount - 1
            if (position >= 0) {
                binding.chatMessages.smoothScrollToPosition(position)
                chatViewModel.unreadMessagesCount.postValue(0)
            }
        }

        if(binding.chatView.visibility == View.VISIBLE) {

            binding.buttonOpenChat.setIconDisabled(R.drawable.ic_chat_message)
        } else {
            binding.buttonOpenChat.setIconEnabled(R.drawable.ic_chat_message)
        }
    }

    private fun startOrStopScreenShare() {
        if (meetingViewModel.isScreenShared()) {
            stopScreenShare()
        } else {
            startScreenShare()
        }
    }

    private fun startScreenShare() {
        val mediaProjectionManager: MediaProjectionManager? = requireContext().getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        resultLauncher.launch(mediaProjectionManager?.createScreenCaptureIntent())
    }

    private fun stopScreenShare() {
        meetingViewModel.stopScreenshare()
    }

    //entry point to start PIP mode
    private fun launchPipMode() {

        activity?.enterPictureInPictureMode()
    }

    val pipActionsMap = mutableMapOf<String, RemoteAction>()

    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setActions(pipActionsMap.map { it.value }.toList())
                    .build()
            )
        }
    }



    private fun openMusicDialog() {
        findNavController().navigate(R.id.musicChooserSheet)
    }

    private fun cleanup() {
        // Because the scope of Chat View Model is the entire activity
        // We need to perform a cleanup
        chatViewModel.clearMessages()
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
                    val recordingState = meetingViewModel.isRecording.value

//                    if (recordingState == RecordingState.NOT_RECORDING_OR_STREAMING && meetingViewModel.isHlsKitUrl
//                    ) {
//
//                        val endCallDialog = Dialog(requireContext())
//                        endCallDialog.setContentView(R.layout.exit_confirmation_dialog)
//                        endCallDialog.findViewById<TextView>(R.id.dialog_title).text =
//                            "Leave Meeting"
//                        endCallDialog.findViewById<TextView>(R.id.dialog_description).text =
//                            "You're about to quit the meeting, are you sure?"
//                        endCallDialog.findViewById<AppCompatButton>(R.id.cancel_btn).text =
//                            "Don’t Leave"
//                        endCallDialog.findViewById<AppCompatButton>(R.id.accept_btn).text = "Leave"
//                        endCallDialog.findViewById<AppCompatButton>(R.id.cancel_btn)
//                            .setOnClickListener { endCallDialog.dismiss() }
//                        endCallDialog.findViewById<AppCompatButton>(R.id.accept_btn)
//                            .setOnClickListener {
//                                endCallDialog.dismiss()
//                                meetingViewModel.leaveMeeting()
//                            }
//                        endCallDialog.show()
//                    } else {
                    inflateExitFlow()
//                    }
                }
            })
    }

    fun roleChangeRemote() {

        val isAllowedToMuteUnmute =
            meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers()
        var remotePeersAreMute: Boolean? = null
        if (isAllowedToMuteUnmute) {
            remotePeersAreMute = meetingViewModel.areAllRemotePeersMute()
        }

        val cancelRoleName = "Cancel"
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
                            meetingViewModel.remoteMute(
                                !remotePeersAreMute,
                                listOf(stringRole)
                            )
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Nothing
                }

            })
        binding.roleSpinner.root.performClick()
    }

    fun inflateExitFlow() {
        if (meetingViewModel.isAllowedToEndMeeting() || (meetingViewModel.isAllowedToHlsStream() && meetingViewModel.isHlsRunning())) {
            MultipleLeaveOptionBottomSheet()
                .show(childFragmentManager, "LeaveBottomSheet")
        } else {
            LeaveCallBottomSheet().show(parentFragmentManager, null)
        }
    }
}