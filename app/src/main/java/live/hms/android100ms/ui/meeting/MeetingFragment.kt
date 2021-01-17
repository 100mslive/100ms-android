package live.hms.android100ms.ui.meeting

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.brytecam.lib.*
import com.brytecam.lib.payload.HMSPayloadData
import com.brytecam.lib.payload.HMSPublishStream
import com.brytecam.lib.payload.HMSStreamInfo
import com.brytecam.lib.webrtc.HMSRTCMediaStream
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints
import com.brytecam.lib.webrtc.HMSStream
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils
import com.google.android.material.snackbar.Snackbar
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.chat.ChatMessage
import live.hms.android100ms.ui.chat.ChatViewModel
import live.hms.android100ms.util.SettingsStore
import live.hms.android100ms.util.viewLifecycle
import org.appspot.apprtc.AppRTCAudioManager
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class MeetingFragment : Fragment(), HMSEventListener {

    companion object {
        private const val TAG = "MeetingFragment"
        private const val RC_CALL = 111

        private const val FRONT_FACING_CAMERA = "user"
        private const val REAR_FACING_CAMERA = "environment"
    }

    private var binding by viewLifecycle<FragmentMeetingBinding>()
    private val args: MeetingFragmentArgs by navArgs()

    private lateinit var settingsStore: SettingsStore
    private lateinit var roomDetails: RoomDetails

    private var shouldReconnect = false
    private var isJoined = false
    private var isPublished = false
    private var retryCount = 0

    private var isFrontCameraEnabled = true
    private var isCameraToggled = false
    private var isAudioEnabled = true
    private var isVideoEnabled = true

    private var currentDeviceTrack: MeetingTrack? = null
    private val meetingTracks = ArrayList<MeetingTrack>()
    private var pinnedTrack: MeetingTrack? = null

    private val executor = Executors.newSingleThreadExecutor()

    private var hmsClient: HMSClient? = null
    private var hmsRoom: HMSRoom? = null
    private var hmsPeer: HMSPeer? = null
    private var localMediaConstraints: HMSRTCMediaStreamConstraints? = null
    private var localMediaStream: HMSRTCMediaStream? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()

    private lateinit var clipboard: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        roomDetails = args.roomDetail
        clipboard = requireActivity()
            .getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager


        initAudioManager()
        initClient()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingBinding.inflate(inflater, container, false)

        settingsStore = SettingsStore(requireContext())

        initRecyclerView()
        binding.containerPinView.visibility = View.GONE

        initButtons()
        initOnBackPress()

        return binding.root
    }

    private fun initViewModel() {
        meetingViewModel.selectedOption.observe(viewLifecycleOwner) { onMeetingOptionSelected(it) }

        chatViewModel.setSendBroadcastCallback { message ->
            Log.v(TAG, "Sending broadcast: $message via $hmsClient")
            hmsClient?.broadcast(message.message, hmsRoom, object : HMSRequestHandler {
                override fun onSuccess(s: String?) {
                    Log.v(TAG, "Successfully broadcast message=${message.message} (s=$s)")
                }

                override fun onFailure(errorCode: Long, errorMessage: String) {
                    Toast.makeText(
                        requireContext(),
                        "Cannot send '${message}'. Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.v(
                        TAG,
                        "Cannot broadcast message=${message} code=${errorCode} errorMessage=${errorMessage}"
                    )
                }
            })
        }
    }

    private fun initAudioManager() {
        val manager = AppRTCAudioManager.create(requireContext())
        Log.d(TAG, "Starting Audio manager")

        manager.start { selectedAudioDevice, availableAudioDevices ->
            Log.d(
                TAG,
                "onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice"
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_CALL)
    private fun initClient() {
        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            initHMSClient()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Need User permissions to proceed",
                RC_CALL,
                *perms
            )
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = MeetingTrackAdapter(requireContext(), meetingTracks, { track ->
                Snackbar.make(
                    binding.root,
                    "Name: ${track.peer.userName} (${track.peer.role}) \nId: ${track.peer.customerUserId}",
                    Snackbar.LENGTH_LONG,
                ).setAction("Copy") {
                    val clip = ClipData.newPlainText("Customer Id", track.peer.customerUserId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        requireContext(),
                        "Copied customer id of ${track.peer.userName} to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()
            }, { pinTrack(it) })
        }
    }

    private fun pinTrack(track: MeetingTrack) {
        binding.pinnedSurfaceView.apply {
            if (binding.containerPinView.visibility == View.VISIBLE) {
                // Already some other view is pinned
                release()
                clearImage()
                pinnedTrack?.videoTrack?.removeSink(this)
                pinnedTrack = null
            }

            init(HMSWebRTCEglUtils.getRootEglBaseContext(), null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
            pinnedTrack = track
            pinnedTrack?.videoTrack?.addSink(this)
        }

        binding.name.text = track.peer.userName
        binding.containerPinView.apply {
            if (visibility == View.GONE) visibility = View.VISIBLE
        }

        val view = TextureView(requireContext())

    }

    private fun getUserMedia(
        frontCamEnabled: Boolean,
        audioEnabled: Boolean,
        cameraToggle: Boolean
    ) {
        localMediaConstraints = HMSRTCMediaStreamConstraints(true, settingsStore.publishVideo)
        localMediaConstraints?.apply {
            videoCodec = settingsStore.codec
            videoFrameRate = settingsStore.videoFrameRate.toInt()
            videoResolution = settingsStore.videoResolution
            videoMaxBitRate = settingsStore.videoBitrate.toInt()

            if (frontCamEnabled) {
                isFrontCameraEnabled = true
                cameraFacing = FRONT_FACING_CAMERA
            } else {
                cameraFacing = REAR_FACING_CAMERA
            }

            hmsClient?.getUserMedia(
                requireContext(),
                localMediaConstraints,
                object : HMSClient.GetUserMediaListener {
                    override fun onSuccess(mediaStream: HMSRTCMediaStream?) {
                        Log.v(TAG, "GetUserMedia Success")
                        localMediaStream = mediaStream
                        // TODO: Init my surface view (on UI Thread)

                        var videoTrack: VideoTrack? = null
                        var audioTrack: AudioTrack? = null

                        mediaStream?.stream?.apply {
                            if (videoTracks.isNotEmpty()) {
                                videoTrack = videoTracks[0]
                                videoTrack?.setEnabled(settingsStore.publishVideo)
                            }
                            if (audioTracks.isNotEmpty()) {
                                audioTrack = audioTracks[0]
                                audioTrack?.setEnabled(settingsStore.publishAudio)
                            }

                            currentDeviceTrack = MeetingTrack(
                                hmsPeer!!,
                                videoTrack,
                                audioTrack,
                                true
                            )
                            meetingTracks.add(0, currentDeviceTrack!!)

                            requireActivity().runOnUiThread {
                                // binding.recyclerView.adapter?.notifyItemInserted(0)
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                            }
                        }

                        if (!isPublished) {
                            hmsClient?.publish(
                                localMediaStream,
                                hmsRoom,
                                localMediaConstraints,
                                object : HMSStreamRequestHandler {
                                    override fun onSuccess(data: HMSPublishStream?) {
                                        Log.v(TAG, "Publish Success ${data!!.mid}")
                                        isPublished = true
                                    }

                                    override fun onFailure(errorCode: Long, errorReason: String?) {
                                        Log.v(TAG, "Publish Failure $errorCode $errorReason")
                                    }
                                })
                        }
                    }

                    override fun onFailure(errorCode: Long, errorReason: String?) {
                        Log.v(TAG, "GetUserMedia failed: $errorCode $errorReason")
                    }
                })
        }
    }

    private fun initHMSClient() {
        hmsPeer = HMSPeer(roomDetails.username, roomDetails.authToken)
        hmsRoom = HMSRoom(roomDetails.roomId)
        val config = HMSClientConfig(roomDetails.endpoint)
        hmsClient = HMSClient(this, requireContext(), hmsPeer, config).apply {
            setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
            connect()
        }
    }

    private fun onMeetingOptionSelected(option: MeetingOptions) {
        Log.v(TAG, "onMeetingOptionSelected(option=${option})")
        when (option) {
            MeetingOptions.NONE -> {
            }
            MeetingOptions.END_CALL -> {
                disconnect()
            }
            MeetingOptions.FLIP_CAMERA -> {
                hmsClient?.apply {
                    isCameraToggled = true
                    switchCamera()
                }
            }
            MeetingOptions.TOGGLE_AUDIO -> {
                currentDeviceTrack?.apply {
                    if (audioTrack != null) {
                        isAudioEnabled = !audioTrack.enabled()
                        audioTrack.setEnabled(isAudioEnabled)
                    }
                }
            }
            MeetingOptions.TOGGLE_VIDEO -> {
                currentDeviceTrack?.apply {
                    if (videoTrack != null) {
                        isVideoEnabled = !videoTrack.enabled()
                        videoTrack.setEnabled(isVideoEnabled)
                        executor.execute {
                            if (isVideoEnabled) HMSStream.getCameraCapturer().start()
                            else HMSStream.getCameraCapturer().stop()
                        }
                    }
                }
            }
            MeetingOptions.OPEN_CHAT -> {
                findNavController().navigate(
                    MeetingFragmentDirections.actionMeetingFragmentToChatFragment(roomDetails)
                )
            }
            MeetingOptions.SHARE -> {
                val meetingUrl = roomDetails.let {
                    "https://${it.env}.100ms.live/?room=${it.roomId}&env=${it.env}&role=Guest"
                }
                val clip = ClipData.newPlainText("Meeting Link", meetingUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    requireContext(),
                    "Copied meeting link to clipboard",
                    Toast.LENGTH_SHORT
                ).show()
            }
            MeetingOptions.OPEN_SETTINGS -> {
                // TODO: Go to settings fragment
                Toast.makeText(
                    requireContext(),
                    "Work in progress",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Hacky-fix: Since onBackPress from any child fragment
        // the observer fires the last event received
        // Eg: OPEN_CHAT is called again onBackPress which open the chat again
        if (option != MeetingOptions.NONE) {
            meetingViewModel.selectOption(MeetingOptions.NONE)
        }
    }

    private fun initButtons() {
        binding.buttonUnpin.setOnClickListener {
            val visible = binding.containerPinView.visibility == View.VISIBLE
            binding.pinnedSurfaceView.apply {
                release()
                clearImage()
                pinnedTrack?.videoTrack?.removeSink(this)
                pinnedTrack = null
            }

            if (visible) {
                binding.containerPinView.visibility = View.GONE
            }
        }

        binding.fabMoreOptions.setOnClickListener {
            val metadata = MeetingOptionsMetadata(isAudioEnabled, isVideoEnabled)
            findNavController().navigate(
                MeetingFragmentDirections.actionMeetingFragmentToMeetingOptionsBottomSheet(metadata)
            )
        }
    }

    private fun disconnect() {
        isJoined = false
        isPublished = false

        try {
            hmsClient?.leave(object : HMSRequestHandler {
                override fun onSuccess(s: String?) {
                    Log.v(TAG, "On Leave Success")
                }

                override fun onFailure(l: Long, s: String?) {
                    Log.v(TAG, "On Leave Failure")
                }
            })
            HMSStream.stopCapturers()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // TODO: Cleanup

        localMediaStream = null
        hmsClient?.disconnect()

        // Because the scope of Chat View Model is the entire activity
        // We need to perform a cleanup
        chatViewModel.removeSendBroadcastCallback()
        chatViewModel.clearMessages()

        findNavController().navigate(
            MeetingFragmentDirections.actionMeetingFragmentToHomeFragment()
        )
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
                    disconnect()
                }
            })
    }

    // HMS Event Listener events below
    override fun onConnect() {
        shouldReconnect = false
        requireActivity().runOnUiThread {
            // TODO: Make Reconnect Progress view invisible
        }

        retryCount = 0
        Log.v(TAG, "onConnect");

        if (!isJoined) {
            hmsClient?.join(object : HMSRequestHandler {
                override fun onSuccess(p0: String?) {
                    isJoined = true
                    Log.v(TAG, "Join Success")
                    getUserMedia(
                        isFrontCameraEnabled,
                        settingsStore.publishAudio,
                        isCameraToggled
                    )
                }

                override fun onFailure(p0: Long, p1: String?) {
                    Log.v(TAG, "Join Failure")
                }
            })
        }
    }

    override fun onDisconnect(errorMessage: String?) {
        Log.v(TAG, "onDisconnect: $errorMessage")
        shouldReconnect = true
        isJoined = false
        isPublished = false

        // TODO: Clean up of views

        localMediaStream = null
        localAudioTrack = null
        localVideoTrack = null

        // TODO: Init Reconnect
    }

    override fun onPeerJoin(peer: HMSPeer) {
        Log.v(
            TAG,
            "onPeerJoin: uid=${peer.uid}, role=${peer.role}, userId=${peer.customerUserId}, peerId=${peer.peerId}"
        )
        Toast.makeText(
            requireContext(),
            "${peer.userName} - ${peer.peerId} joined",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPeerLeave(peer: HMSPeer) {
        Log.v(
            TAG,
            "onPeerLeave: uid=${peer.uid}, role=${peer.role}, userId=${peer.customerUserId}, peerId=${peer.peerId}"
        )
        Toast.makeText(
            requireContext(),
            "${peer.userName} - ${peer.peerId} left",
            Toast.LENGTH_SHORT
        ).show()
    }


    override fun onStreamAdd(peer: HMSPeer, streamInfo: HMSStreamInfo) {
        Log.v(
            TAG,
            "onStreamAdd: peer-uid:${peer.uid}, role=${peer.role}, userId:${peer.customerUserId}"
        )

        // Handling all the on stream add events inside a single thread to avoid race condition during rendering
        executor.execute {
            hmsClient?.subscribe(streamInfo, hmsRoom, object : HMSMediaRequestHandler {
                override fun onSuccess(stream: MediaStream) {
                    var videoTrack: VideoTrack? = null
                    var audioTrack: AudioTrack? = null

                    if (stream.videoTracks.size > 0) {
                        videoTrack = stream.videoTracks[0]
                        videoTrack.setEnabled(true)
                    }

                    if (stream.audioTracks.size > 0) {
                        audioTrack = stream.audioTracks[0]
                        audioTrack.setEnabled(true)
                    }

                    meetingTracks.add(MeetingTrack(peer, videoTrack, audioTrack))
                    requireActivity().runOnUiThread {
                        // binding.recyclerView.adapter?.notifyItemInserted(meetingTracks.size - 1)
                        binding.recyclerView.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onFailure(errorCode: Long, errorReason: String) {
                    Log.v(TAG, "onStreamAdd: subscribe failure: $errorCode $errorReason")
                }
            })
        }
    }

    override fun onStreamRemove(streamInfo: HMSStreamInfo) {
        Log.v(TAG, "onStreamRemove: ${streamInfo.uid}")

        // Get the index of the meeting track having uid
        var idx = -1
        for (i in 0..meetingTracks.size step 1) {
            if (meetingTracks[i].peer.uid.equals(streamInfo.uid, true)) {
                idx = i
                break
            }
        }

        if (idx == -1) {
            Log.v(TAG, "onStreamRemove: ${streamInfo.uid} not found in meeting tracks")
            return
        }

        meetingTracks.removeAt(idx)
        requireActivity().runOnUiThread {
            // binding.recyclerView.adapter?.notifyItemRemoved(idx)
            binding.recyclerView.adapter?.notifyDataSetChanged()

        }
    }

    override fun onBroadcast(data: HMSPayloadData) {
        Log.v(
            TAG,
            "onBroadcast: customerId=${data.peer.customerUserId} senderName=${data.senderName} msg=${data.msg}"
        )
        requireActivity().runOnUiThread {
            val senderName = data.senderName ?: "error<senderName=null>"
            chatViewModel.receivedMessage(
                ChatMessage(
                    senderName,
                    Date(),
                    data.msg,
                    false
                )
            )
        }
    }
}