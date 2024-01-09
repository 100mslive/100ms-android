package live.hms.roomkit.ui.meeting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.animation.ControlFocusInsetsAnimationCallback
import live.hms.roomkit.animation.TranslateDeferringInsetsAnimationCallback
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.drawableStart
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.hideKeyboard
import live.hms.roomkit.initAnimState
import live.hms.roomkit.setDrawables
import live.hms.roomkit.startBounceAnimationUpwards
import live.hms.roomkit.ui.meeting.participants.ParticipantsAdapter
import live.hms.roomkit.ui.meeting.participants.ParticipantsDialog
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.buttonDisabled
import live.hms.roomkit.ui.theme.buttonEnabled
import live.hms.roomkit.ui.theme.getPreviewLayout
import live.hms.roomkit.ui.theme.setIconDisabled
import live.hms.roomkit.ui.theme.setIconEnabled
import live.hms.roomkit.util.NameUtils
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.openShareIntent
import live.hms.roomkit.util.setCameraGestureListener
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.switchCamera
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRecordingState
import live.hms.video.sdk.models.enums.HMSStreamingState
import live.hms.video.sdk.models.role.PublishParams
import live.hms.videoview.VideoViewStateChangeListener


class PreviewFragment : Fragment() {

    companion object {
        private const val TAG = "PreviewFragment"
    }

    private var binding by viewLifecycle<FragmentPreviewBinding>()

    private lateinit var settings: SettingsStore

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    private var alertDialog: AlertDialog? = null

    private var track: MeetingTrack? = null

    private var isViewVisible = false

    private var participantsDialog: ParticipantsDialog? = null
    private var participantsDialogAdapter: ParticipantsAdapter? = null

    private var setTextOnce = false
    private var isPreviewLoaded = false
    private var nameEditText: String? = null
    private var isBeamRunning = false
    private var isFirstRender : Boolean = true
    private var startHlsStream : Boolean = false



    private fun updateJoinButtonTextIfHlsIsEnabled(roleName: String?): Boolean {
        val hlsJoinButtonFromLayoutConfig = meetingViewModel.getHmsRoomLayout()
            ?.getPreviewLayout(roleName)?.default?.elements?.joinForm?.joinBtnType == "JOIN_BTN_TYPE_JOIN_AND_GO_LIVE"

        if (isBeamRunning.not() && hlsJoinButtonFromLayoutConfig) {
            if (binding.buttonJoinMeeting.drawableStart == null) {
                binding.buttonJoinMeeting.setDrawables(
                    start = ContextCompat.getDrawable(
                        context!!, R.drawable.ic_live
                    )
                )
            }
            binding.buttonJoinMeeting.text = "Go Live"
            startHlsStream = true
            return true
        } else {
            binding.buttonJoinMeeting.text = "Join Now"
            binding.buttonJoinMeeting.setDrawables(
                start = null            )

            startHlsStream = false
            return false
        }

    }

    override fun onResume() {
        super.onResume()
        isViewVisible = true
        bindVideo()
    }

    override fun onPause() {
        super.onPause()
        isViewVisible = false
        unbindVideo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun bindVideo(isFirstRender: Boolean = false) {
        if (track?.video?.isMute == false) {
            track?.video?.let {
                binding.previewView.addTrack(it)
                binding.previewView.setCameraGestureListener(it, {
                    activity?.openShareIntent(it)
                }, {})
            }
            if (isFirstRender) {
                binding.previewView.initAnimState(alphaOnly = true)
                binding.previewView.visibility = View.VISIBLE
                binding.previewView.addVideoViewStateChangeListener(object : VideoViewStateChangeListener {
                    override fun onFirstFrameRendered() {
                        super.onFirstFrameRendered()
                        contextSafe { context, activity ->
                            activity.runOnUiThread {
                                if (isFirstRender) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        binding?.previewGradient?.startBounceAnimationUpwards(animationDuration = 650, interpolator = AccelerateDecelerateInterpolator())
                                        binding?.previewView?.startBounceAnimationUpwards()
                                    }
                                }
                            }
                        }
                    }
                })
            } else {
                binding.previewView.visibility = View.VISIBLE
            }

        } else {
            binding.previewView.visibility = View.GONE
        }

    }

    private fun unbindVideo() {
        binding.previewView.visibility = View.GONE
        binding.previewView.removeTrack()
    }

    private fun enableDisableJoinNowButton() {
        if (isPreviewLoaded && nameEditText.isNullOrEmpty().not()) {
            binding.buttonJoinMeeting.buttonEnabled()
        } else {
            binding.buttonJoinMeeting.buttonDisabled()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         initIntroAnimation()
         initOnBackPress()
         initButtons()
         initObservers()


        if (savedInstanceState != null) {
            // Recreated Fragment
            meetingViewModel.roomLayoutLiveData.observe(viewLifecycleOwner) {success ->
                if (success) {
                    meetingViewModel.roomLayoutLiveData.removeObservers(viewLifecycleOwner)
                    initUI()
                    meetingViewModel.startPreview()
                    enableDisableJoinNowButton()

                } else {
                    this.activity?.finish()
                }
            }
        } else {
            initUI()
            meetingViewModel.startPreview()
            enableDisableJoinNowButton()
        }


    }

    fun initUI() {
        binding.applyTheme()
        with(binding.editTextName) {
            isEnabled = !meetingViewModel.disableNameEdit()
            if(!isEnabled) {
                binding.editContainerName.isHintEnabled = false
            } else {
                hint = "Enter Name…"
            }
        }
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())
        startFixedIntroAnimation()
        setupKeyboardAnimation()
    }

    private fun startFixedIntroAnimation() {
        var animationOffset = 350
        binding.apply {
            previewBottomBar.startBounceAnimationUpwards(animationOffset)
            animationOffset += 50
            editContainerName.startBounceAnimationUpwards(animationOffset)
            buttonJoinMeeting.startBounceAnimationUpwards(animationOffset)
        }
    }

    private fun initIntroAnimation() {
        with(binding) {
            previewBottomBar.initAnimState()
            buttonToggleAudio.initAnimState(littleLessTranslate = true)
            buttonToggleVideo.initAnimState(littleLessTranslate = true)
            buttonSwitchCamera.initAnimState(littleLessTranslate = true)
            buttonNetworkQuality.initAnimState(littleLessTranslate = true)
            iconOutputDevice.initAnimState(littleLessTranslate = true)
            editContainerName.initAnimState()
            buttonJoinMeeting.initAnimState()
            hlsSession.initAnimState(isUpwardsAnimation = false)
            iconParticipants.initAnimState(isUpwardsAnimation = false)
            previewGradient.initAnimState(alphaOnly = true)
            descriptionTv.initAnimState(alphaOnly = true)
            logoIv.initAnimState(alphaOnly = true)
            nameTv.initAnimState(alphaOnly = true)


        }
    }

    private fun setupUI(roleName: String) {
        // don't allow editing the name if there's supposed to be a fixed one

        var introAnimationOffset = 450
        if (!meetingViewModel.liveClassName.isNullOrEmpty()){
            binding.nameTv.text = buildString {
                append(getString(R.string.welcome_text))
                append(" ")
                append(meetingViewModel.liveClassName)
            }
            binding.nameTv.startBounceAnimationUpwards()
        }else{
            binding.nameTv.text = meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout(roleName)?.default?.elements?.previewHeader?.title
            binding.nameTv.startBounceAnimationUpwards()
        }
/*        if (meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout(roleName)?.default?.elements?.previewHeader?.title.isNullOrEmpty()
        ) {
            binding.nameTv.visibility = View.GONE
        } else {
            binding.nameTv.text = meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout(roleName)?.default?.elements?.previewHeader?.title
            binding.nameTv.startBounceAnimationUpwards()
        }*/

        if (meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout(roleName)?.default?.elements?.previewHeader?.subTitle.isNullOrEmpty()
        ) {
            binding.descriptionTv.visibility = View.GONE
        } else {
            binding.descriptionTv.text = meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout(roleName)?.default?.elements?.previewHeader?.subTitle
            binding.descriptionTv.startBounceAnimationUpwards()
        }

        if(!meetingViewModel.roomLogoUrl.isNullOrEmpty()){
            binding.logoIv.visibility = View.VISIBLE
            Glide.with(this)
                .load(meetingViewModel.roomLogoUrl)
                .into(binding.logoIv);
            introAnimationOffset += 50
            binding.logoIv.startBounceAnimationUpwards()
        }else{
            binding.logoIv.visibility = View.INVISIBLE
        }

        /***
        if (meetingViewModel.getHmsRoomLayout()?.getCurrentRoleData(roleName)?.logo?.url.isNullOrEmpty()) {
        binding.logoIv.visibility = View.INVISIBLE
        } else {
        binding.logoIv.visibility = View.VISIBLE
        Glide.with(this)
        .load(meetingViewModel.getHmsRoomLayout()?.getCurrentRoleData(roleName)?.logo?.url)
        .into(binding.logoIv);
        introAnimationOffset += 50
        binding.logoIv.startBounceAnimationUpwards()

        }***/
    }

    private fun setupKeyboardAnimation() {

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.previewBottomBar, TranslateDeferringInsetsAnimationCallback(
                view = binding.previewBottomBar,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime(),
                // We explicitly allow dispatch to continue down to binding.messageHolder's
                // child views, so that step 2.5 below receives the call
                dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            )
        )

        val movableOnKeybaordOpen = arrayOf(binding.buttonNetworkQuality)
        movableOnKeybaordOpen.forEach {
            ViewCompat.setWindowInsetsAnimationCallback(
                it, TranslateDeferringInsetsAnimationCallback(
                    view = it,
                    persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                    deferredInsetTypes = WindowInsetsCompat.Type.ime()
                )
            )

        }

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.editTextName, ControlFocusInsetsAnimationCallback(binding.editTextName)
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        setupParticipantsDialog()
    }


    private fun setupParticipantsDialog() {
        participantsDialog = ParticipantsDialog()
        participantsDialogAdapter = participantsDialog?.adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initButtons() {

        binding.closeBtn.setOnSingleClickListener(300L) {
            contextSafe { context, activity ->
                meetingViewModel.leaveMeeting()
                goToHomePage()
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

        binding.iconNoiseCancellation.setOnSingleClickListener(200L) {
                meetingViewModel.toggleNcInPreview.value = meetingViewModel.toggleNcInPreview.value!!.not()
                updateNoiseCancellationIcon()
            }

        binding.iconOutputDevice.setOnSingleClickListener(200L) {
                Log.v(TAG, "iconOutputDevice.onClick()")

                AudioOutputSwitchBottomSheet { audioDevice, isMuted ->
                    if (isMuted)
                        updateActionVolumeMenuIcon()
                }.show(
                    childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                )
            }

        binding.buttonSwitchCamera.setOnSingleClickListener(200L) {
            if (it.isEnabled) track?.video.switchCamera()
        }


        binding.buttonToggleVideo.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleVideo.onClick()")

                (track?.video as HMSLocalVideoTrack?)?.let {
                    if (it.isMute) {
                        // Un-mute this track
                        it.setMute(false)
                        if (isViewVisible) {
                            bindVideo()
                        }
                        binding.buttonSwitchCamera.alpha = 1f
                        binding.buttonSwitchCamera.isEnabled = true
                        binding.buttonToggleVideo.setIconEnabled(R.drawable.avd_video_off_to_on)
//
                    } else {
                        // Mute this track
                        it.setMute(true)
                        if (isViewVisible) {
                            unbindVideo()
                        }
                        binding.buttonSwitchCamera.alpha = 0.5f
                        binding.buttonSwitchCamera.isEnabled = false
                        binding.buttonToggleVideo.setIconDisabled(R.drawable.avd_video_on_to_off)
                    }
                } ?: run {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.preview_screen_icon_disabled_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.buttonToggleAudio.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleAudio.onClick()")

                (track?.audio as HMSLocalAudioTrack?)?.let {
                    it.setMute(!it.isMute)

                    if (it.isMute) {
                        binding.buttonToggleAudio.setIconDisabled(R.drawable.avd_mic_on_to_off)
                    } else {
                        binding.buttonToggleAudio.setIconEnabled(R.drawable.avd_mic_off_to_on)
                    }
                } ?: run {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.preview_screen_icon_disabled_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.buttonJoinMeeting.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonJoinMeeting.onClick()")
                if (this.isEnabled) {
                    hideKeyboard()

                    meetingViewModel.updateNameInPreview(
                        binding.editTextName.text.toString().trim()
                    )

                    //start meeting
                    if (meetingViewModel.state.value is MeetingState.Disconnected) {
                        meetingViewModel.startMeeting()
                    }
                }
            }
        }
    }

    private fun enableJoinLoader() {
        binding.joinLoader.visibility = View.VISIBLE
//        binding.editContainerName.isEnabled = false
      //  binding.editTextName.isEnabled = false
    }

    private fun disableJoinLoader() {
        binding.joinLoader.visibility = View.INVISIBLE
      //  binding.editContainerName.isEnabled = true
       // binding.editTextName.isEnabled = true
    }

    private fun navigateToMeeting() {
        findNavController().navigate(
            PreviewFragmentDirections.actionPreviewFragmentToMeetingFragment(startHlsStream = startHlsStream)
        )
    }

    private fun updateActionVolumeMenuIcon(
        audioOutputType: HMSAudioManager.AudioDevice? = null
    ) {
        binding.iconOutputDevice.visibility = View.VISIBLE
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

    private fun updateNoiseCancellationIcon() {
        with(binding.iconNoiseCancellation) {
            if(meetingViewModel.toggleNcInPreview.value == true) {
                setIconDisabled(R.drawable.reduce_noise_session_option)
            } else
            {
                setIconEnabled(R.drawable.reduce_noise_session_option)
            }

            val existingVisibility = visibility
            visibility = if(meetingViewModel.displayNoiseCancellationButton()) View.VISIBLE else View.GONE
            if(existingVisibility == View.GONE && visibility == View.VISIBLE) {
                startBounceAnimationUpwards()
            }
        }
    }
    private fun updateActionVolumeMenuIcon() {
        binding.iconOutputDevice.visibility = View.VISIBLE
        binding.iconOutputDevice.apply {
            if (meetingViewModel.isPeerAudioEnabled()) {
                setIconEnabled(R.drawable.ic_icon_speaker)
            } else {
                setIconDisabled(R.drawable.ic_volume_off_24)
            }
        }
    }


    private fun goToHomePage() {
        requireActivity().finish()
    }

    private fun initObservers() {

        meetingViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is MeetingState.Connecting, is MeetingState.Reconnecting, is MeetingState.Joining, is MeetingState.PublishingMedia -> {
                    enableJoinLoader()
                }

                is MeetingState.Failure -> {
                    disableJoinLoader()
                    it.exceptions.forEach { exception ->
                        meetingViewModel.triggerErrorNotification(exception.message, isDismissible = false)
                    }
                }

                is MeetingState.ForceLeave -> {
                    meetingViewModel.leaveMeeting()
                    goToHomePage()
                }

                is MeetingState.Ongoing, is MeetingState.Reconnected -> {
                    disableJoinLoader()
                    navigateToMeeting()
                }

                else -> {

                }
            }
        }

        meetingViewModel.previewRoomStateLiveData.observe(viewLifecycleOwner) {
            if (it.second.peerCount != null) {
                binding.iconParticipants.visibility = View.VISIBLE
                binding.participantCountText.text = it.second.peerCount.formatNames().orEmpty()
                binding.iconParticipants.startBounceAnimationUpwards()
            }
            isBeamRunning = it.second.hlsStreamingState.state == HMSStreamingState.STARTED ||
                    it.second.rtmpHMSRtmpStreamingState.state == HMSStreamingState.STARTED ||
                    it.second.browserRecordingState.state == HMSRecordingState.STARTED
            updateJoinButtonTextIfHlsIsEnabled(it?.second?.localPeer?.hmsRole?.name)

            val isLiveWithHLSOrRTMP = it.second.hlsStreamingState.state == HMSStreamingState.STARTED ||
                    it.second.rtmpHMSRtmpStreamingState.state == HMSStreamingState.STARTED
            if ((isLiveWithHLSOrRTMP) && (meetingViewModel.isLiveIconEnabled == true || meetingViewModel.isLiveIconEnabled == null)) {
                binding.liveHlsGroup.visibility = View.VISIBLE
                binding.hlsSession.startBounceAnimationUpwards()
            } else {
                binding.liveHlsGroup.visibility = View.GONE
            }
        }

        meetingViewModel.previewErrorLiveData.observe(viewLifecycleOwner) { error ->
            if (error.isTerminal) {
                isPreviewLoaded = false
               meetingViewModel.triggerErrorNotification(error.message, isDismissible = false)
            } else {
                meetingViewModel.triggerErrorNotification(error.message)
            }
        }

        meetingViewModel.previewPeerLiveData.observe(viewLifecycleOwner) { (type, peer) ->
            when (type) {
                HMSPeerUpdate.PEER_JOINED -> {
                    participantsDialogAdapter?.insertItem(peer)
                }

                HMSPeerUpdate.PEER_LEFT -> {
                    participantsDialogAdapter?.removeItem(peer)
                }

                HMSPeerUpdate.NETWORK_QUALITY_UPDATED -> {
                    peer.networkQuality?.downlinkQuality?.let {
                        binding.networkQuality.visibility = View.VISIBLE
                        updateNetworkQualityView(it, requireContext(), binding.networkQuality)
                    }
                }

                else -> Unit
            }
        }

        meetingViewModel.previewUpdateLiveData.observe(viewLifecycleOwner,
            Observer { (room, localTracks) ->

                if (setTextOnce.not()) {
                    setupUI(room.localPeer?.hmsRole?.name.orEmpty())
                    binding.nameInitials.visibility = View.VISIBLE
                    binding.nameInitials.text = NameUtils.getInitials(room.localPeer!!.name)
                    binding.editTextName.setText(
                        room.localPeer?.name.orEmpty(), TextView.BufferType.EDITABLE
                    )
                    nameEditText = room.localPeer?.name.orEmpty()
                    enableDisableJoinNowButton()
                    setTextOnce = true
                }
                isPreviewLoaded = true
                enableDisableJoinNowButton()

                updateUiBasedOnPublishParams(room.localPeer?.hmsRole?.publishParams)
                track = MeetingTrack(room.localPeer!!, null, null)
                localTracks.forEach {
                    when (it) {
                        is HMSLocalAudioTrack -> {
                            track?.audio = it
                        }

                        is HMSLocalVideoTrack -> {
                            track?.video = it

                            if (isViewVisible) {
                                bindVideo(isFirstRender = isFirstRender)
                            }
                            isFirstRender = false
                        }
                    }
                }

                binding.editTextName.doOnTextChanged { text, start, before, count ->
                    if (text.isNullOrEmpty().not()) {
                        val intitals = kotlin.runCatching { NameUtils.getInitials(text.toString()) }
                        binding.nameInitials.text = intitals.getOrNull().orEmpty()
                        binding.noNameIv.visibility = View.GONE
                    } else {
                        binding.nameInitials.text = ""
                        binding.noNameIv.visibility = View.VISIBLE
                    }
                    nameEditText = text.toString()
                    enableDisableJoinNowButton()
                }

                // Disable buttons
                track?.video?.let {
                    binding.buttonToggleVideo.apply {
                        isEnabled = (track?.video != null)

                        if (it.isMute) {
                            binding.buttonSwitchCamera.alpha = 0.5f
                            binding.buttonSwitchCamera.isEnabled = false
                            setIconDisabled(R.drawable.avd_video_on_to_off)
                        } else {
                            binding.buttonSwitchCamera.alpha = 1f
                            binding.buttonSwitchCamera.isEnabled = true
                            setIconEnabled(R.drawable.avd_video_off_to_on)
                        }
                    }
                }

                updateNoiseCancellationIcon()
                if (settings.lastUsedMeetingUrl.contains("/streaming/").not()) {

                    updateJoinButtonTextIfHlsIsEnabled(room.localPeer?.hmsRole?.name)
                    enableDisableJoinNowButton()
                    binding.buttonJoinMeeting.visibility = View.VISIBLE

                } else {
                    updateActionVolumeMenuIcon()
                    binding.buttonJoinMeeting.visibility = View.VISIBLE
                }

                track?.audio?.let {
                    binding.buttonToggleAudio.apply {
                        isEnabled = (track?.audio != null)

                        if (it.isMute) {
                            binding.buttonToggleAudio.setIconDisabled(R.drawable.avd_mic_on_to_off)
                        } else {
                            binding.buttonToggleAudio.setIconEnabled(R.drawable.avd_mic_off_to_on)
                        }
                    }
                }
            })

    }

    private fun updateUiBasedOnPublishParams(publishParams: PublishParams?) {
        if (publishParams == null) return

        /**
        if (publishParams.allowed.contains("audio")) {
            binding.buttonToggleAudio.visibility = View.VISIBLE
            updateNoiseCancellationIcon()
            binding.buttonToggleAudio.startBounceAnimationUpwards()
            binding.iconOutputDevice.startBounceAnimationUpwards()
        } else {
            binding.buttonToggleAudio.visibility = View.GONE
        }

        if (publishParams.allowed.contains("video")) {
            binding.buttonToggleVideo.visibility = View.VISIBLE
            binding.buttonSwitchCamera.visibility = View.VISIBLE

            binding.buttonToggleVideo.startBounceAnimationUpwards()
            binding.buttonSwitchCamera.startBounceAnimationUpwards()

            binding.videoCardContainer.visibility = View.VISIBLE
        } else {
            binding.topMarging.setGuidelinePercent(0.35f)
            binding.buttonToggleVideo.visibility = View.GONE
            binding.buttonSwitchCamera.visibility = View.GONE
            binding.videoCardContainer.visibility = View.GONE
            binding.iconNoiseCancellation.visibility = View.GONE
        }
         **/

        with(binding) {
            val audioAllowed = "audio" in publishParams.allowed
            val videoAllowed = "video" in publishParams.allowed

            iconOutputDevice.startBounceAnimationUpwards()
            setViewVisibleAndBounceAnimation(
                view = buttonToggleAudio,
                isViewEnabled = audioAllowed
            )
            setViewVisibleAndBounceAnimation(
                view = buttonToggleVideo,
                isViewEnabled = videoAllowed
            )
            setViewVisibleAndBounceAnimation(
                view = buttonSwitchCamera,
                isViewEnabled = videoAllowed
            )
            videoCardContainer.visibility = if (videoAllowed) View.VISIBLE else View.GONE
            if (!videoAllowed) topMarging.setGuidelinePercent(0.35f)
        }
    }
    private fun setViewVisibleAndBounceAnimation(view: View, isViewEnabled: Boolean) {
        view.visibility = View.VISIBLE
        view.startBounceAnimationUpwards(forceAlpha = if (isViewEnabled) 1.0f else 0.5f)
    }

    private fun updateNetworkQualityView(
        downlinkScore: Int, context: Context, imageView: ImageView
    ) {
        NetworkQualityHelper.getNetworkResource(downlinkScore, context = requireContext())
            .let { drawable ->
                imageView.setImageDrawable(drawable)
                if (drawable == null) {
                    imageView.visibility = View.GONE
                    binding.buttonNetworkQuality.visibility = View.GONE
                } else {
                    imageView.visibility = View.VISIBLE
                    binding.buttonNetworkQuality.visibility = View.VISIBLE
                    binding.buttonNetworkQuality.startBounceAnimationUpwards()
                }
            }
    }

    private fun getRemotePeers(hmsRoom: HMSRoom): ArrayList<HMSPeer> {
        val previewPeerList = arrayListOf<HMSPeer>()
        hmsRoom.peerList.forEach {
            if (it !is HMSLocalPeer) {
                previewPeerList.add(it)
            }
        }
        return previewPeerList
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    meetingViewModel.leaveMeeting()
                    goToHomePage()
                }
            })
    }
}

private fun Int?.formatNames(): String? {
    if (this == null) return null
    return if (this == 0) {
        "You are the first to join"
    } else "${this} others in session"
}

fun HMSRoom.isHLSRoom(): Boolean {
    return this.hlsStreamingState?.variants?.size ?: 0 > 0 && this.hlsStreamingState?.variants?.get(
        0
    )?.hlsStreamUrl.isNullOrEmpty().not()

}
