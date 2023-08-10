package live.hms.roomkit.ui.meeting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import live.hms.roomkit.R
import live.hms.roomkit.animation.ControlFocusInsetsAnimationCallback
import live.hms.roomkit.animation.TranslateDeferringInsetsAnimationCallback
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.drawableStart
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.hideKeyboard
import live.hms.roomkit.setDrawables
import live.hms.roomkit.ui.meeting.participants.ParticipantsAdapter
import live.hms.roomkit.ui.meeting.participants.ParticipantsDialog
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.*
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.*
import live.hms.video.audio.HMSAudioManager
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.role.PublishParams
import live.hms.video.utils.HMSLogger


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
    private var audioOutputIcon: MenuItem? = null

    private var participantsDialog: ParticipantsDialog? = null
    private var participantsDialogAdapter: ParticipantsAdapter? = null

    private var setTextOnce = false
    private var isPreviewLoaded = false
    private var nameEditText: String? = null
    private var isHlsRunning = false
    private var isHlsPermission = false



    private fun updateJoinButtonTextIfHlsIsEnabled() : Boolean {
        val hlsJoinButtonFromLayoutConfig = meetingViewModel.getHmsRoomLayout()
            ?.getPreviewLayout()?.default?.elements?.joinForm?.joinBtnType == "JOIN_BTN_TYPE_JOIN_AND_GO_LIVE"

        if (isHlsPermission && isHlsRunning.not() && hlsJoinButtonFromLayoutConfig) {
            if (binding.buttonJoinMeeting.drawableStart == null) {
                binding.buttonJoinMeeting.setDrawables(
                    start = ContextCompat.getDrawable(
                        context!!, R.drawable.ic_live
                    )
                )
            }
            binding.buttonJoinMeeting.text = "Go Live"
            return true
        } else {
            binding.buttonJoinMeeting.text = "Join Now"
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

    private fun bindVideo() {
        if (track?.video?.isMute == false) {
            track?.video?.let {
                binding.previewView.addTrack(it)
                binding.previewView.setCameraGestureListener(it, {
                    activity?.openShareIntent(it)
                }, {})
            }
            binding.previewView.visibility = View.VISIBLE
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
        binding.applyTheme()
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())
        setupUI()

        setupKeyboardAnimation()

        enableDisableJoinNowButton()

        meetingViewModel.hmsSDK.setAudioDeviceChangeListener(object :
            HMSAudioManager.AudioManagerDeviceChangeListener {
            override fun onAudioDeviceChanged(
                device: HMSAudioManager.AudioDevice?,
                listOfDevices: MutableSet<HMSAudioManager.AudioDevice>?
            ) {
                audioOutputIcon?.let {
                    if (meetingViewModel.isPeerAudioEnabled()) {
                        updateActionVolumeMenuIcon(device)
                    }
                }
            }

            override fun onError(error: HMSException?) {
                HMSLogger.d(TAG, "error : ${error?.description}")
            }
        })


    }

    private fun setupUI() {
        if (meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout()?.default?.elements?.previewHeader?.title.isNullOrEmpty()
        ) {
            binding.nameTv.visibility = View.GONE
        } else {
            binding.nameTv.text = meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout()?.default?.elements?.previewHeader?.title
        }

        if (meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout()?.default?.elements?.previewHeader?.subTitle.isNullOrEmpty()
        ) {
            binding.descriptionTv.visibility = View.GONE
        } else {
            binding.descriptionTv.text = meetingViewModel.getHmsRoomLayout()
                ?.getPreviewLayout()?.default?.elements?.previewHeader?.subTitle
        }


        if (meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url.isNullOrEmpty()) {
            binding.logoIv.visibility = View.INVISIBLE
        } else {
            binding.logoIv.visibility = View.VISIBLE
            Glide.with(this)
                .load(meetingViewModel.getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url)
                .into(binding.logoIv);
        }

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

        initOnBackPress()
        initButtons()
        initObservers()
        meetingViewModel.startPreview()

        return binding.root
    }

    private fun initButtons() {

        meetingViewModel.previewRoomStateLiveData.observe(viewLifecycleOwner) {
            if (it.second.peerCount != null) {
                binding.iconParticipants.visibility = View.VISIBLE
                binding.participantCountText.text = it.second.peerCount.formatNames().orEmpty()
            }
            updateJoinButtonTextIfHlsIsEnabled()
            isHlsRunning = it.second.hlsStreamingState?.running == true
            isHlsPermission = it.second.localPeer?.hmsRole?.permission?.hlsStreaming ?: false

            if (it.second.hlsStreamingState?.running == true) {
                binding.liveHlsGroup.visibility = View.VISIBLE
            } else {
                binding.liveHlsGroup.visibility = View.GONE
            }


        }

        binding.closeBtn.setOnSingleClickListener(300L) {
            contextSafe { context, activity ->
                meetingViewModel.leaveMeeting()
                goToHomePage()
            }
        }


        binding.iconOutputDevice.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "iconParticipants.onClick()")


                AudioOutputSwitchBottomSheet({ audioDevice, isMuted ->
                    updateActionVolumeMenuIcon(audioDevice)
                }).show(
                    childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                )


            }
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
        binding.editContainerName.isEnabled = false
        binding.editTextName.isEnabled = false
    }

    private fun disableJoinLoader() {
        binding.joinLoader.visibility = View.INVISIBLE
        binding.editContainerName.isEnabled = true
        binding.editTextName.isEnabled = true
    }

    private fun navigateToMeeting() {
        findNavController().navigate(
            PreviewFragmentDirections.actionPreviewFragmentToMeetingFragment(updateJoinButtonTextIfHlsIsEnabled())
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


    private fun goToHomePage() {/*Intent(requireContext(), HomeActivity::class.java).apply {
            crashlyticsLog(
                TAG,
                "MeetingActivity.finish() -> going to HomeActivity :: $this"
            )
            startActivity(this)
        }*/
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
                    contextSafe { context, activity ->
                        Toast.makeText(
                            activity, "${it.exceptions}", Toast.LENGTH_LONG
                        ).show()
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

        meetingViewModel.previewErrorLiveData.observe(viewLifecycleOwner) { error ->
            if (error.isTerminal) {
                isPreviewLoaded = false
                enableDisableJoinNowButton()
                AlertDialog.Builder(requireContext()).setTitle(error.name)
                    .setMessage(error.toString()).setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        goToHomePage()
                    }.setNeutralButton(R.string.bug_report) { _, _ ->
                        requireContext().startActivity(
                            EmailUtils.getNonFatalLogIntent(requireContext())
                        )
                        alertDialog = null
                    }.create().show()
            } else {
                Toast.makeText(context, error.description, Toast.LENGTH_LONG).show()
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
                                bindVideo()
                            }
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

                if (settings.lastUsedMeetingUrl.contains("/streaming/").not()) {

                    updateJoinButtonTextIfHlsIsEnabled()
                    enableDisableJoinNowButton()
                    binding.buttonJoinMeeting.visibility = View.VISIBLE
                    updateActionVolumeMenuIcon(meetingViewModel.hmsSDK.getAudioOutputRouteType())
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


        if (publishParams.allowed.contains("audio")) {
            binding.buttonToggleAudio.visibility = View.VISIBLE
        } else {
            binding.buttonToggleAudio.visibility = View.GONE
        }

        if (publishParams.allowed.contains("video")) {
            binding.buttonToggleVideo.visibility = View.VISIBLE
            binding.buttonSwitchCamera.visibility = View.VISIBLE
            binding.videoCardContainer.visibility = View.VISIBLE
        } else {
            binding.topMarging.setGuidelinePercent(0.35f)
            binding.buttonToggleVideo.visibility = View.GONE
            binding.buttonSwitchCamera.visibility = View.GONE
            binding.videoCardContainer.visibility = View.GONE
        }
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
