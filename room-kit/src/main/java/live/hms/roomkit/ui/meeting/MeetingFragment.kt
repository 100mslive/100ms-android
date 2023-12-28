package live.hms.roomkit.ui.meeting

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
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
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.setGradient
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.filters.FilterBottomSheet
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
import live.hms.roomkit.ui.meeting.chat.combined.CHAT_TAB_TITLE
import live.hms.roomkit.ui.meeting.chat.combined.ChatParticipantCombinedFragment
import live.hms.roomkit.ui.meeting.chat.combined.ChatRbacRecipientHandling
import live.hms.roomkit.ui.meeting.chat.combined.LaunchMessageOptionsDialog
import live.hms.roomkit.ui.meeting.chat.combined.OPEN_TO_CHAT_ALONE
import live.hms.roomkit.ui.meeting.chat.combined.OPEN_TO_PARTICIPANTS
import live.hms.roomkit.ui.meeting.chat.combined.PinnedMessageUiUseCase
import live.hms.roomkit.ui.meeting.chat.rbac.RoleBasedChatBottomSheet
import live.hms.roomkit.ui.meeting.commons.VideoGridBaseFragment
import live.hms.roomkit.ui.meeting.participants.ParticipantsFragment
import live.hms.roomkit.ui.meeting.pinnedvideo.PinnedVideoFragment
import live.hms.roomkit.ui.meeting.videogrid.VideoGridFragment
import live.hms.roomkit.ui.notification.HMSNotificationType
import live.hms.roomkit.ui.polls.leaderboard.millisecondsToDisplayTime
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
import live.hms.video.sdk.models.enums.HMSRecordingState
import live.hms.video.sdk.models.enums.HMSStreamingState


val LEAVE_INFORMATION_PERSON = "bundle-leave-information-person"
val LEAVE_INFORMATION_REASON = "bundle-leave-information-reason"
val LEAVE_INFROMATION_WAS_END_ROOM = "bundle-leave-information-end-room"

class MeetingFragment : Fragment() {
    companion object {
        private const val TAG = "MeetingFragment"
        const val AudioSwitchBottomSheetTAG = "audioSwitchBottomSheet"
    }

    private var binding by viewLifecycle<FragmentMeetingBinding>()
    private lateinit var currentFragment: Fragment
    private var hasStartedHls: Boolean = false
    private val pinnedMessageUiUseCase = PinnedMessageUiUseCase()

    private lateinit var settings: SettingsStore
    var countDownTimer: CountDownTimer? = null
    var isCountdownManuallyCancelled: Boolean = false

    private val args: MeetingFragmentArgs by navArgs()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }
    private val launchMessageOptionsDialog = LaunchMessageOptionsDialog()
    private val chatAdapter by lazy {
        ChatAdapter({ message ->
            launchMessageOptionsDialog.launch(meetingViewModel,
            childFragmentManager, message) }, ::onChatClick, { message -> MessageOptionsBottomSheet.showMessageOptions(meetingViewModel, message)})
    }

    private val chatViewModel: ChatViewModel by activityViewModels<ChatViewModel> {
        ChatViewModelFactory(meetingViewModel.hmsSDK)
    }



    private var isMeetingOngoing = false

    private val onSettingsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (SettingsStore.APPLY_CONSTRAINTS_KEYS.contains(key)) {
                // meetingViewModel.updateLocalMediaStreamConstraints()
            }
        }

    override fun onResume() {
        super.onResume()
        isCountdownManuallyCancelled = false
        setupStreamingTimeView()
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

    private fun onChatClick() {
        if (controlBarsVisible && meetingViewModel.prebuiltInfoContainer.isChatOverlay())
            hideControlBars()
        else
            showControlBars(true)
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
        hasStartedHls = false
        meetingViewModel.setCountDownTimerStartedAt(null)
        countDownTimer?.cancel()
    }

    override fun onPause() {
        super.onPause()
        isCountdownManuallyCancelled = true
        meetingViewModel.setCountDownTimerStartedAt(null)
        countDownTimer?.cancel()
    }

    override fun onStart() {
        super.onStart()
        isCountdownManuallyCancelled = false
        setupStreamingTimeView()
    }

    private fun updateActionVolumeMenuIcon(
        audioOutputType: HMSAudioManager.AudioDevice? = null
    ) {
        if (meetingViewModel.isPeerAudioEnabled().not()) {
            binding.iconOutputDevice.setIconEnabled(R.drawable.ic_volume_off_24)
            return
        }
        binding.iconOutputDevice.apply {
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

    private fun setupStreamingTimeView() {
        countDownTimer?.cancel()
        meetingViewModel.setCountDownTimerStartedAt(null)
        countDownTimer = object : CountDownTimer(1000, 1000) {
            override fun onTick(l: Long) {
                val startedAt =
                   meetingViewModel.hmsSDK.getRoom()?.hlsStreamingState?.variants?.firstOrNull()?.startedAt
                        ?: meetingViewModel.hmsSDK.getRoom()?.rtmpHMSRtmpStreamingState?.startedAt
                startedAt?.let {
                    if (startedAt > 0) {
                        meetingViewModel.setCountDownTimerStartedAt(startedAt)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()

        if (savedInstanceState != null) {
            // Recreated Fragment
            meetingViewModel.roomLayoutLiveData.observe(viewLifecycleOwner) {success ->
                if (success) {
                    meetingViewModel.state.observe(viewLifecycleOwner) { state ->
                        if (state is MeetingState.Ongoing) {
                            hideProgressBar()
                            isMeetingOngoing = true
                            meetingViewModel.state.removeObservers(viewLifecycleOwner)
                            initializeUI()
                        }
                    }
                    meetingViewModel.roomLayoutLiveData.removeObservers(viewLifecycleOwner)
                    meetingViewModel.startMeeting()
                } else {
                    this.activity?.finish()
                }
            }
        } else {
            // To handle skip_preview -> we need to call join Directly and wait for the response before showing the UI
            meetingViewModel.roomLayoutLiveData.observe(viewLifecycleOwner) {success ->
                if (success) {
                    meetingViewModel.state.observe(viewLifecycleOwner) { state ->
                        if (state is MeetingState.Disconnected)
                            meetingViewModel.startMeeting()
                        if (state is MeetingState.Ongoing) {
                            hideProgressBar()
                            isMeetingOngoing = true
                            meetingViewModel.state.removeObservers(viewLifecycleOwner)
                            initializeUI()
                            startHLSStreamingIfRequired()
                        }
                    }
                    meetingViewModel.roomLayoutLiveData.removeObservers(viewLifecycleOwner)

                } else {
                    this.activity?.finish()
                }
            }
        }
    }

    private fun initializeUI() {
        initButtons()
        initObservers()
        initOnBackPress()

        meetingViewModel.countDownTimerStartedAt.observe(viewLifecycleOwner) { startedAt ->
            if (startedAt != null) {
                binding.tvStreamingTime.visibility = View.VISIBLE
                binding.tvStreamingTime.text =
                    millisecondsToDisplayTime(System.currentTimeMillis().minus(startedAt))
            } else {
                binding.tvStreamingTime.visibility = View.GONE
            }
        }
        binding.chatMessages.isHeightContrained = true
        PauseChatUIUseCase().setChatPauseVisible(
            binding.chatOptionsCard,
            meetingViewModel
        )
        pinnedMessageUiUseCase.init(binding.pinnedMessagesRecyclerView, binding.pinCloseButton, meetingViewModel::unPinMessage, meetingViewModel.isAllowedToPinMessages())
        ChatUseCase().initiate(
            chatViewModel.messages,
            meetingViewModel.chatPauseState,
            meetingViewModel.roleChange,
            meetingViewModel.currentBlockList,
            viewLifecycleOwner,
            chatAdapter,
            binding.chatMessages,
            chatViewModel,
            meetingViewModel,
            null,
            binding.iconSend,
            binding.editTextMessage,
            binding.userBlocked,
            binding.chatPausedBy,
            binding.chatPausedContainer,
            binding.chatExtra,
            meetingViewModel.prebuiltInfoContainer::isChatEnabled,
            meetingViewModel::availableRecipientsForChat,
            chatViewModel::currentlySelectedRbacRecipient,
            chatViewModel.currentlySelectedRecipientRbac,
        )
        meetingViewModel.peerLeaveUpdate.observe(viewLifecycleOwner) {
            chatViewModel.updatePeerLeave(it)
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

        return binding.root
    }

    private fun goToHomePage(details: HMSRemovedFromRoom? = null) {

        //only way to programmatically dismiss pip mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (activity?.isInPictureInPictureMode == true) {
                activity?.moveTaskToBack(false)
            }
        }

        requireActivity().finish()
    }

    private fun updateRecordingViews(state: HMSRecordingState, shouldShowRecordingIcons : Boolean?) {
        if (shouldShowRecordingIcons == false){
            binding.recordingSignalProgress.visibility = View.GONE
            binding.recordingSignal.visibility = View.GONE
            binding.recordingPause.visibility = View.GONE
            return
        }

        when (state) {
            HMSRecordingState.STARTING -> {
                binding.recordingSignalProgress.visibility = View.VISIBLE
                binding.recordingSignal.visibility = View.GONE
                binding.recordingPause.visibility = View.GONE
            }
            HMSRecordingState.RESUMED, HMSRecordingState.STARTED -> {
                binding.recordingSignalProgress.visibility = View.GONE
                binding.recordingSignal.visibility = View.VISIBLE
                binding.recordingPause.visibility = View.GONE
            }
            HMSRecordingState.PAUSED -> {
                binding.recordingSignalProgress.visibility = View.GONE
                binding.recordingSignal.visibility = View.GONE
                binding.recordingPause.visibility = View.VISIBLE
            }
            HMSRecordingState.FAILED, HMSRecordingState.NONE, HMSRecordingState.STOPPED -> {
                binding.recordingSignalProgress.visibility = View.GONE
                binding.recordingSignal.visibility = View.GONE
                binding.recordingPause.visibility = View.GONE
            }
        }
    }

    private fun updateStreamingViews(state: HMSStreamingState) {
        when (state) {
            HMSStreamingState.STARTING -> {
                // Remove the loader on getting STARTING notification for any peer who has hlsStreaming permission
                if (meetingViewModel.isAllowedToHlsStream()) {
                    binding.meetingFragmentProgress.visibility = View.GONE
                }
            }

            HMSStreamingState.STARTED -> {
                binding.meetingFragmentProgress.visibility = View.GONE

                /** binding.liveTitleCard.visibility = View.VISIBLE **/
                val liveTitleCardVisibility = if (meetingViewModel.isLiveIconEnabled==false) View.GONE else View.VISIBLE
                binding.liveTitleCard.visibility = liveTitleCardVisibility

                if (meetingViewModel.isRTMPRunning()) {
                    binding.liveTitle.text = "Live with RTMP"
                } else {
                    binding.liveTitle.text = "Live"
                }
                binding.tvViewersCountCard.visibility = View.VISIBLE
                binding.tvViewersCount.visibility = View.VISIBLE
                setupStreamingTimeView()
            }

            HMSStreamingState.NONE, HMSStreamingState.STOPPED, HMSStreamingState.FAILED -> {
                if (state != HMSStreamingState.NONE)
                    binding.meetingFragmentProgress.visibility = View.GONE
                binding.liveTitleCard.visibility = View.GONE
                binding.tvViewersCount.visibility = View.GONE
                binding.tvViewersCountCard.visibility = View.GONE
            }

        }
    }

    private fun initObservers() {
        binding.sendToBackground.setOnSingleClickListener {
            RoleBasedChatBottomSheet.launch(childFragmentManager, chatViewModel)
        }
        // This only needs to be in meetingfragment since we always open it.
        // Is that true for HLS? Double check.
        meetingViewModel.showAudioIcon.observe(viewLifecycleOwner) { visible ->
            binding.iconOutputDevice.visibility = if(visible) View.VISIBLE else View.GONE
        }
        meetingViewModel.initPrebuiltChatMessageRecipient.observe(viewLifecycleOwner) {
            chatViewModel.setInitialRecipient(it.first, it.second)
            ChatRbacRecipientHandling().updateChipRecipientUI(binding.sendToChipText, it.first)
        }
        chatViewModel.currentlySelectedRecipientRbac.observe(viewLifecycleOwner) { recipient ->
            ChatRbacRecipientHandling().updateChipRecipientUI(binding.sendToChipText, recipient)
            // if recipient is null, hide the chat.
            // but recipient might be null if they're just selecting from roles/participants as well.

        }
        meetingViewModel.messageIdsToHide.observe(viewLifecycleOwner) { messageIdsToHide ->
            chatViewModel.updateMessageHideList(messageIdsToHide)
        }
        meetingViewModel.currentBlockList.observe(viewLifecycleOwner) { chatBlockedPeerIdsList ->
            chatViewModel.updateBlockList(chatBlockedPeerIdsList)
        }
        meetingViewModel.showHlsStreamYetToStartError.observe(viewLifecycleOwner) { showError ->
                binding.streamYetToStartContainer.visibility = if (showError) View.VISIBLE else View.GONE
        }

        meetingViewModel.peerCount.observe(viewLifecycleOwner) {
            binding.tvViewersCount.text =it.toString()

        }

        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }

        meetingViewModel.pinnedMessages.observe(viewLifecycleOwner) { pinnedMessages ->
            pinnedMessageUiUseCase.messagesUpdate(pinnedMessages,
                binding.pinnedMessagesDisplay)
        }

        meetingViewModel.recordingState.observe(viewLifecycleOwner) { state ->
            updateRecordingViews(state , meetingViewModel.isRecordingIconsEnabled)
        }

        meetingViewModel.streamingState.observe(viewLifecycleOwner) { state ->
            updateStreamingViews(state)
        }

        meetingViewModel.isHandRaised.observe(viewLifecycleOwner) { isHandRaised ->
            if (isHandRaised) {
                binding.buttonRaiseHand.setIconDisabled(R.drawable.ic_raise_hand)
            } else {
                binding.buttonRaiseHand.setIconEnabled(R.drawable.ic_raise_hand)
            }
        }

        meetingViewModel.isScreenShare.observe(viewLifecycleOwner) {
            meetingViewModel.triggerScreenShareNotification(it)
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
                        Log.i("RecordingState", event.message)
                    }
                    is MeetingViewModel.Event.ServerRecordEvent -> {
                        Log.i("RecordingState", event.message)
                    }
                    is MeetingViewModel.Event.HlsEvent, is MeetingViewModel.Event.HlsRecordingEvent -> {
                        Log.d("RecordingState", "HlsEvent: ${event}")
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
            (binding.buttonToggleAudio as? ShapeableImageView)?.apply {

                if (enabled) {
                    setIconEnabled(R.drawable.avd_mic_off_to_on)
                } else {
                    setIconDisabled(R.drawable.avd_mic_on_to_off)
                }
            }
        }

        meetingViewModel.roleChange.observe(viewLifecycleOwner) {
            updateChatButtonWhenRoleChanges()
        }

    }

    private fun startHLSStreamingIfRequired() {
        val canStartHlsStreamFromConfig = meetingViewModel.getHmsRoomLayout()
            ?.getPreviewLayout(null)?.default?.elements?.joinForm?.joinBtnType == "JOIN_BTN_TYPE_JOIN_AND_GO_LIVE"
        if (canStartHlsStreamFromConfig && meetingViewModel.isAllowedToHlsStream() && meetingViewModel.isHlsRunning().not()) {
            binding.meetingFragmentProgress.visibility = View.VISIBLE
            hasStartedHls = true
            meetingViewModel.startHls(settings.lastUsedMeetingUrl, HMSHlsRecordingConfig(true, false))
        }
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

        //handle orientation change
        when(mode) {
            is MeetingViewMode.HLS_VIEWER -> {
                    contextSafe { context, activity -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR }
            }
            else -> {
                contextSafe { context, activity -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, currentFragment)
            .addToBackStack(null)
            .commit()




        if(modeEnteredOrExitedHls) {
            val isChatEnabled = meetingViewModel.prebuiltInfoContainer.isChatEnabled()
            val isChatOverlay = meetingViewModel.prebuiltInfoContainer.isChatOverlay()
            val isChatOpenByDefault = meetingViewModel.prebuiltInfoContainer.chatInitialStateOpen()
            val chatVisible = isChatEnabled && isChatOverlay && isChatOpenByDefault
            toggleChatVisibility(chatVisible)
            if(chatVisible)
                moveChat(up = true, bottomMenuHeight = binding.bottomControls.height.toFloat())
            else
                moveChat(up = false, binding.topMenu.height.toFloat())
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


        binding.topMenu.visibility = View.VISIBLE
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



        binding.topMenu.setBackgroundColor(
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
        binding.buttonRaiseHand.visibility = View.GONE

        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)

        showSystemBars()
    }

    private fun configureHLSView() {
        // They aren't present by default when
        //  the view starts in hls
        // But will be present after role changes
        //  so it's important to attempt to hide them.
        hideControlBars()
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

        binding.buttonRaiseHand.visibility = View.VISIBLE

        binding.fragmentContainer.setOnSingleClickListener(500L) {
            // The bars are disabled in hls fragment view
            if(meetingViewModel.meetingViewMode.value is MeetingViewMode.HLS_VIEWER)
                return@setOnSingleClickListener
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
                override fun onAnimationStart(animation: Animator) {
                    binding.topMenu.visibility = View.VISIBLE
                    showSystemBars()
                    // This prevents the bar from moving twice as high as it should
                    if(shouldHideAfterDelay)
                        moveChat(up = true, bottomMenuHeight = binding.bottomControls.height.toFloat())
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.topMenu.visibility = View.VISIBLE
                    controlBarsVisible = true
                    if (shouldHideAfterDelay) {
                        // Hide control bars
                        delayedHide(3000)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    binding.topMenu.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator) {

                }

            })?.start()

        binding.bottomControls.animate()
            ?.translationY(0f)?.setDuration(300)?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    binding.bottomControls.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.bottomControls.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationCancel(animation: Animator) {
                    binding.bottomControls.visibility = View.VISIBLE
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator) {

                }

            })?.start()
    }

    private fun moveChat(up: Boolean, bottomMenuHeight: Float) {
        if(binding.chatView.visibility != View.VISIBLE)
            return

        with(binding.chatView!!){
            if(up) {
                (layoutParams as RelativeLayout.LayoutParams).apply {
//                    removeRule(RelativeLayout.ALIGN_BOTTOM)
//                    addRule(RelativeLayout.ABOVE, R.id.bottom_controls)
//                    updateMargins(bottom = bottomMenuHeight.toInt().let { if(it == 0) 130 else it } + 8.dp())
                }
            } else {
                (layoutParams as RelativeLayout.LayoutParams).apply {
//                    removeRule(RelativeLayout.ABOVE)
//                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, R.id.meeting_container)
//                    updateMargins(bottom = 8.dp())
                }
            }
        }

    }
    private fun hideControlBars() {
        val topMenu = binding.topMenu
        val bottomMenu = binding.bottomControls
        val screenHeight = requireActivity().window.decorView.height
        controlBarsVisible = false
        topMenu.animate()
            ?.translationY(-(topMenu.height.toFloat()))?.setDuration(300)
            ?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    moveChat(up = false, bottomMenu.height.toFloat())
                }

                override fun onAnimationEnd(animation: Animator) {
                    topMenu.visibility = View.GONE
                    controlBarsVisible = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator) {

                }

            })?.start()

        bottomMenu.animate()
            ?.translationY((bottomMenu.height.toFloat()))?.setDuration(300)
            ?.setListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    bottomMenu.visibility = View.GONE
                    controlBarsVisible = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    controlBarsVisible = true
                }

                override fun onAnimationRepeat(animation: Animator) {

                }

            })?.start()
    }

    private fun delayedHide(delayMillis: Int) {
        cancelCallback()
        handler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    private fun hideProgressBar() {
        var isInPIPMode = false
        binding.fragmentContainer.visibility = View.VISIBLE
        if (!isInPIPMode && (meetingViewModel.meetingViewMode.value is MeetingViewMode.HLS_VIEWER).not()){
            binding.bottomControls.visibility = View.VISIBLE
        }
        binding.progressBar.root.visibility = View.GONE
    }

    private fun showProgressBar() {
        binding.fragmentContainer.visibility = View.VISIBLE
        if(meetingViewModel.meetingViewMode.value !is MeetingViewMode.HLS_VIEWER)
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

        binding.iconSend.setOnSingleClickListener {
            val messageStr = binding.editTextMessage.text.toString().trim()
            if (messageStr.isNotEmpty()) {
                chatViewModel.sendMessage(messageStr)
                binding.editTextMessage.setText("")
            }
        }

        binding.buttonSettingsMenu.apply {

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
                                        putString(CHAT_TAB_TITLE, meetingViewModel.chatTitle())
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
                        onNameChange = {                 FilterBottomSheet().show(
                            childFragmentManager,
                            ChangeNameDialogFragment.TAG
                        )
                        },
                        showPolls = { findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToPollsCreationFragment()) },
                        onRecordingClicked = {
                            val isBrowserRecordingRunning = meetingViewModel.hmsSDK.getRoom()?.browserRecordingState?.state in listOf(
                                HMSRecordingState.STARTING, HMSRecordingState.STARTED,
                                HMSRecordingState.RESUMED, HMSRecordingState.PAUSED
                            )
                            if (isBrowserRecordingRunning.not()) {
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
                        onNoiseClicked = meetingViewModel::toggleNoiseCancellation
                    ).show(
                        childFragmentManager, AudioSwitchBottomSheetTAG
                    )

                } else {
                    val settingsBottomSheet = SettingsBottomSheet(meetingViewModel, {
                        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToParticipantsFragment())
                    }, {
                        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToRoleChangeFragment())
                    },
                        {
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
                    arguments = Bundle().apply {
                        putBoolean(OPEN_TO_CHAT_ALONE,
                        !meetingViewModel.isParticpantListEnabled()
                    )
                        putString(CHAT_TAB_TITLE, meetingViewModel.chatTitle())
                    }
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


        binding.iconOutputDevice.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "iconOutputDevice.onClick()")

                AudioOutputSwitchBottomSheet { audioDevice, isMuted ->

                    if (isMuted)
                        updateActionVolumeMenuIcon()
                }.show(
                    childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                )
            }
        }
        updateActionVolumeMenuIcon(meetingViewModel.getAudioOutputRouteType())
        meetingViewModel.hmsSDK.setAudioDeviceChangeListener(object :
            HMSAudioManager.AudioManagerDeviceChangeListener {
            override fun onAudioDeviceChanged(
                p0: HMSAudioManager.AudioDevice,
                p1: Set<HMSAudioManager.AudioDevice>
            ) {
                meetingViewModel.updateAudioDeviceChange(p0)
            }


            override fun onError(p0: HMSException) {
            }
        })

        meetingViewModel.audioDeviceChange.observe(viewLifecycleOwner, Observer{
            updateActionVolumeMenuIcon(it)
        })


        binding.buttonSwitchCamera.setOnSingleClickListener(200L) {
            meetingViewModel.flipCamera()
            if (it.isEnabled) meetingViewModel.flipCamera()
        }

        if (!meetingViewModel.roomLogoUrl.isNullOrEmpty()){
            binding.logoIv.visibility = View.VISIBLE
            binding.logoIv.let {
                Glide.with(this)
                    .load(meetingViewModel.roomLogoUrl)
                    .into(it)
            }
        }else{
            binding.logoIv.visibility = View.GONE
        }

        /***
        if (meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url.isNullOrEmpty()) {
        binding.logoIv?.visibility = View.GONE
        } else {
        binding.logoIv.visibility = View.VISIBLE
        binding.logoIv.let {
        Glide.with(this)
        .load(meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url)
        .into(it)
        }
        }
         ***/

    }

    private fun isOverlayChatVisible() : Boolean {
        return binding.chatView.visibility == View.VISIBLE
    }
    private fun toggleChatVisibility(forceState : Boolean? = null) {
        with(binding.chatView) {
            visibility = if(forceState == null) {
                if (visibility == View.GONE) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } else {
                if(forceState) View.VISIBLE else View.GONE
            }
        }
        binding.chatMessages.visibility = binding.chatView.visibility
        // Because the meeting fragment can toggle the
        //  chat visibility and this applies
        //  whether the chat is enabled or blocked or paused
        //  we need a different way to show this UI, independent
        //  of whether the UI should be hidden for chat RBAC feature reasons.
        // So the UI that chat RBAC triggers is put into a wrapper.
        // when this toggle button toggles hide/view it changes the
        //  wrapper visibility, which means chat can be enabled
        //  and controlled entirely by ChatUseCase but also hidden
        //  since we hide the wrapper that contains it.
        binding.pinnedMessagesWrapper.visibility = binding.chatView.visibility
        binding.chatExtraWrapper.visibility = binding.chatView.visibility
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
                    inflateExitFlow()
                }
            })
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