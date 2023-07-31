package live.hms.roomkit.ui.meeting

import android.app.ProgressDialog.show
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
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.drawableStart
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.setDrawables
import live.hms.roomkit.ui.meeting.participants.ParticipantsAdapter
import live.hms.roomkit.ui.meeting.participants.ParticipantsDialog
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.*
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setBackgroundAndColor
import live.hms.roomkit.util.*
import live.hms.video.audio.HMSAudioManager
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.utils.HMSCoroutineScope
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

    //TODO get from the config api
    private val startLiveStreamIng by lazy { meetingViewModel.isGoLiveInPreBuiltEnabled() }

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

        meetingViewModel.previewUpdateLiveData.observe(viewLifecycleOwner) {
            binding.liveHlsGroup.visibility = if (it.first.isHLSRoom()) View.VISIBLE else View.GONE
            binding.iconParticipants.text = it.first.peerList.formatNames()
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
            PreviewFragmentDirections.actionPreviewFragmentToMeetingFragment(startLiveStreamIng)
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
                    binding.buttonJoinMeeting.text = if (meetingViewModel.isPrebuiltDebugMode()) {
                        "Enter Meeting"
                    }
                    else if (startLiveStreamIng) {
                        binding.buttonJoinMeeting.setDrawables(start = ContextCompat.getDrawable(context!!, R.drawable.ic_live))
                        enableDisableJoinNowButton()
                        "Go LIve"
                    } else {
                        "Join Now"
                    }
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

    private fun updateNetworkQualityView(
        downlinkScore: Int, context: Context, imageView: ImageView
    ) {
        NetworkQualityHelper.getNetworkResource(downlinkScore, context = requireContext())
            .let { drawable ->
                imageView.setImageDrawable(drawable)
                if (drawable == null) {
                    imageView.visibility = View.GONE
                } else {
                    imageView.visibility = View.VISIBLE
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

private fun List<HMSPeer>.formatNames(): CharSequence? {
    var text = ""
    var count = 0
    var hasLocalPeer = false
    this.forEach {
        if (it.isLocal) {
            hasLocalPeer = true
        }
    }
    if (hasLocalPeer && this.size <= 1) {
        return "You are the first to join"
    } else return "${this.size - 1} other session in this room"
}

fun HMSRoom.isHLSRoom(): Boolean {
    return this.hlsStreamingState?.variants?.size ?: 0 > 0 && this.hlsStreamingState?.variants?.get(
        0
    )?.hlsStreamUrl.isNullOrEmpty().not()

}
