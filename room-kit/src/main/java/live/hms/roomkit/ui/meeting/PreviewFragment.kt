package live.hms.roomkit.ui.meeting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.ui.meeting.participants.ParticipantsAdapter
import live.hms.roomkit.ui.meeting.participants.ParticipantsDialog
import live.hms.roomkit.ui.permission.PermissionFragmentDirections
import live.hms.roomkit.ui.settings.SettingsStore
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

    private lateinit var track: MeetingTrack

    private var isViewVisible = false
    private var audioOutputIcon: MenuItem? = null

    private var participantsDialog: ParticipantsDialog? = null
    private var participantsDialogAdapter: ParticipantsAdapter? = null

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
        if (this::track.isInitialized && track.video?.isMute == false) {
            track.video?.let {
                binding.previewView.addTrack(it)
                binding.previewView.setCameraGestureListener(it, {
                    activity?.openShareIntent(it)
                },{})
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())

        meetingViewModel.isRecording.observe(viewLifecycleOwner) {
            if (it == RecordingState.STREAMING_AND_RECORDING) {
                binding.recordingText.text =
                    "The session you are about to join is live and being recorded"
                binding.recordingView.visibility = View.VISIBLE
            } else if (meetingViewModel.isHlsRunning()) {
                binding.recordingText.text = "The session you are about to join is live"
                binding.recordingView.visibility = View.VISIBLE
            } else if (meetingViewModel.isRTMPRunning()) {
                binding.recordingText.text = "The session you are about to join is live"
                binding.recordingView.visibility = View.VISIBLE
            } else {
                binding.recordingView.visibility = View.GONE
            }
        }

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)

        initOnBackPress()
        initButtons()
        initObservers()
        meetingViewModel.startPreview()

        return binding.root
    }

    private fun initButtons() {

        binding.iconParticipants.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "iconParticipants.onClick()")

                participantsDialog?.participantCount =
                    meetingViewModel.previewRoomStateLiveData.value?.second?.peerCount ?: 0
                participantsDialog?.show(
                    requireActivity().supportFragmentManager,
                    "participant_dialog"
                )
            }
        }

        binding.iconOutputDevice.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "iconParticipants.onClick()")

                meetingViewModel.let {
                    val audioSwitchBottomSheet =
                        AudioOutputSwitchBottomSheet(it) { audioDevice, isMuted ->
                            updateActionVolumeMenuIcon(audioDevice)
                        }
                    audioSwitchBottomSheet.show(
                        requireActivity().supportFragmentManager,
                        MeetingFragment.AudioSwitchBottomSheetTAG
                    )
                }
            }
        }


        binding.buttonToggleVideo.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleVideo.onClick()")

                (track.video as HMSLocalVideoTrack?)?.let {
                    if (it.isMute) {
                        // Un-mute this track
                        it.setMute(false)
                        if (isViewVisible) {
                            bindVideo()
                        }
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_camera_toggle_on)
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                        binding.buttonToggleVideoBg.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.gray_light
                            )
                        )
                    } else {
                        // Mute this track
                        it.setMute(true)
                        if (isViewVisible) {
                            unbindVideo()
                        }
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_camera_toggle_off)
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.black)
                        binding.buttonToggleVideoBg.setCardBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.white
                            )
                        )
                    }
                }

            }
        }

        binding.buttonToggleAudio.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleAudio.onClick()")

                (track.audio as HMSLocalAudioTrack?)?.let {
                    it.setMute(!it.isMute)

                    if (it.isMute) {
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_audio_toggle_off)
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.black)
                        binding.buttonToggleAudioBg.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                    } else {
                        background =
                            ContextCompat.getDrawable(context, R.drawable.ic_audio_toggle_on)
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                        binding.buttonToggleAudioBg.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.gray_light
                            )
                        )
                    }
                }
            }
        }

        binding.enterMeetingParentView.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonJoinMeeting.onClick()")
                findNavController().navigate(
                    PreviewFragmentDirections.actionPreviewFragmentToMeetingFragment()
                )

//                findNavController().setGraph(R.navigation.meeting_nav_graph)
            }
        }
    }

    private fun updateActionVolumeMenuIcon(
        audioOutputType: HMSAudioManager.AudioDevice? = null
    ) {
        binding.iconOutputDevice.apply {
            when (audioOutputType) {
                HMSAudioManager.AudioDevice.EARPIECE -> {
                    setImageResource(R.drawable.ic_baseline_hearing_24)
                }
                HMSAudioManager.AudioDevice.SPEAKER_PHONE -> {
                    setImageResource(R.drawable.ic_icon_speaker)
                }
                HMSAudioManager.AudioDevice.AUTOMATIC -> {
                    setImageResource(R.drawable.ic_icon_speaker)
                }
                HMSAudioManager.AudioDevice.BLUETOOTH -> {
                    setImageResource(R.drawable.ic_baseline_bluetooth_24)
                }
                HMSAudioManager.AudioDevice.WIRED_HEADSET -> {
                    setImageResource(R.drawable.ic_baseline_headset_24)
                }
                else -> {
                    setImageResource(R.drawable.ic_volume_off_24)
                }
            }
        }
    }

    private fun updateActionVolumeMenuIcon() {
        binding.iconOutputDevice.apply {
            if (meetingViewModel.isPeerAudioEnabled()) {
                setImageResource(R.drawable.ic_icon_speaker)
            } else {
                setImageResource(R.drawable.ic_volume_off_24)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_flip_camera -> {
                if (this::track.isInitialized) {
                    HMSCoroutineScope.launch {
                        (track.video as HMSLocalVideoTrack?)?.switchCamera()
                    }
                }
            }
            R.id.action_participants -> {
                participantsDialog?.participantCount =
                    meetingViewModel.previewRoomStateLiveData.value?.second?.peerCount ?: 0
                participantsDialog?.show(
                    requireActivity().supportFragmentManager,
                    "participant_dialog"
                )
            }
        }

        return false
    }

    private fun goToHomePage() {
        /*Intent(requireContext(), HomeActivity::class.java).apply {
            crashlyticsLog(
                TAG,
                "MeetingActivity.finish() -> going to HomeActivity :: $this"
            )
            startActivity(this)
        }*/
        requireActivity().finish()
    }

    private fun initObservers() {

        meetingViewModel.previewErrorLiveData.observe(viewLifecycleOwner) { error ->
            if (error.isTerminal) {
                binding.buttonJoinMeeting.isEnabled = false
                AlertDialog.Builder(requireContext())
                    .setTitle(error.name)
                    .setMessage(error.toString())
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        goToHomePage()
                    }
                    .setNeutralButton(R.string.bug_report) { _, _ ->
                        requireContext().startActivity(
                            EmailUtils.getNonFatalLogIntent(requireContext())
                        )
                        alertDialog = null
                    }
                    .create()
                    .show()
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

        meetingViewModel.previewUpdateLiveData.observe(
            viewLifecycleOwner,
            Observer { (room, localTracks) ->
                binding.nameInitials.text = NameUtils.getInitials(room.localPeer!!.name)
                binding.buttonJoinMeeting.isEnabled = true

                track = MeetingTrack(room.localPeer!!, null, null)
                localTracks.forEach {
                    when (it) {
                        is HMSLocalAudioTrack -> {
                            track.audio = it
                        }
                        is HMSLocalVideoTrack -> {
                            track.video = it

                            if (isViewVisible) {
                                bindVideo()
                            }
                        }
                    }
                }

                // Disable buttons
                track.video?.let {
                    binding.buttonToggleVideo.apply {
                        isEnabled = (track.video != null)

                        if (it.isMute) {
                            background =
                                ContextCompat.getDrawable(context, R.drawable.ic_camera_toggle_off)
                            backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.black)
                            binding.buttonToggleVideoBg.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.white
                                )
                            )
                        } else {
                            background =
                                ContextCompat.getDrawable(context, R.drawable.ic_camera_toggle_on)
                            backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.white)
                            binding.buttonToggleVideoBg.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.gray_light
                                )
                            )
                        }
                    }
                }

                if (settings.lastUsedMeetingUrl.contains("/streaming/").not()) {
                    binding.buttonJoinMeeting.text = if (meetingViewModel.isPrebuiltDebugFlagEnabled()) "Join" else  "Enter Meeting"
                    binding.buttonJoinMeeting.visibility = View.VISIBLE
                    updateActionVolumeMenuIcon(meetingViewModel.hmsSDK.getAudioOutputRouteType())
                } else {
                    updateActionVolumeMenuIcon()
                    binding.buttonJoinMeeting.visibility = View.VISIBLE
                }

                track.audio?.let {
                    binding.buttonToggleAudio.apply {
                        isEnabled = (track.audio != null)

                        if (it.isMute) {
                            background =
                                ContextCompat.getDrawable(context, R.drawable.ic_audio_toggle_off)
                            backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.black)
                            binding.buttonToggleAudioBg.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.white
                                )
                            )
                        } else {
                            background =
                                ContextCompat.getDrawable(context, R.drawable.ic_audio_toggle_on)
                            backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.white)
                            binding.buttonToggleAudioBg.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.gray_light
                                )
                            )
                        }
                    }
                }
            })

        meetingViewModel.previewRoomStateLiveData.observe(
            viewLifecycleOwner,
            Observer { (_, room) ->
                if (participantsDialog?.isVisible == true) {
                    participantsDialog?.participantCount =
                        meetingViewModel.previewRoomStateLiveData.value?.second?.peerCount ?: 0
                }
                participantsDialogAdapter?.setItems(getRemotePeers(room))
            })
    }

    private fun updateNetworkQualityView(downlinkScore: Int, context: Context, imageView: ImageView) {
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
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    meetingViewModel.leaveMeeting()
                    goToHomePage()
                }
            })
    }
}