package live.hms.app2.ui.meeting

import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.launch
import live.hms.app2.R
import live.hms.app2.databinding.FragmentPreviewBinding
import live.hms.app2.helpers.NetworkQualityHelper
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.home.HomeActivity
import live.hms.app2.ui.meeting.participants.ParticipantsAdapter
import live.hms.app2.ui.meeting.participants.ParticipantsDialog
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
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

    private lateinit var roomDetails: RoomDetails
    private lateinit var settings: SettingsStore

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application,
            requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
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
        roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    }

    private fun bindVideo() {
        if (this::track.isInitialized && track.video?.isMute == false) {
            SurfaceViewRendererUtil.bind(binding.previewView, track)
            binding.previewView.visibility = View.VISIBLE
        } else {
            binding.previewView.visibility = View.GONE
        }
    }

    private fun unbindVideo() {
        binding.previewView.visibility = View.GONE

        if (this::track.isInitialized) {
            SurfaceViewRendererUtil.unbind(binding.previewView, track)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())

        meetingViewModel.isRecording.observe(viewLifecycleOwner) {
            Log.d("PREVIEW_REC", "STATE IS ${it.name}")
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
                                R.color.gray_color
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
                                R.color.gray_color
                            )
                        )
                    }
                }
            }
        }

        binding.buttonJoinMeeting.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonJoinMeeting.onClick()")

                findNavController().setGraph(R.navigation.meeting_nav_graph)
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
        Intent(requireContext(), HomeActivity::class.java).apply {
            crashlyticsLog(
                TAG,
                "MeetingActivity.finish() -> going to HomeActivity :: $this"
            )
            startActivity(this)
        }
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
                        updateNetworkQualityView(it,requireContext(),binding.networkQuality)
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
                                    R.color.gray_color
                                )
                            )
                        }
                    }
                }

                if (settings.lastUsedMeetingUrl.contains("/streaming/").not()) {
                    binding.buttonJoinMeeting.text = "Enter Meeting"
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
                                    R.color.gray_color
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

    fun updateNetworkQualityView(downlinkScore : Int,context: Context,imageView: ImageView){
        NetworkQualityHelper.getNetworkResource(downlinkScore, context = requireContext()).let { drawable ->
            if (downlinkScore == 0) {
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                imageView.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light), android.graphics.PorterDuff.Mode.SRC_IN)
            }
            imageView.setImageDrawable(drawable)
            if (drawable == null){
                imageView.visibility = View.GONE
            }else{
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