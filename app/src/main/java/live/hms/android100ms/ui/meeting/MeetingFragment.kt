package live.hms.android100ms.ui.meeting

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.brytecam.lib.*
import com.brytecam.lib.payload.HMSPayloadData
import com.brytecam.lib.payload.HMSPublishStream
import com.brytecam.lib.payload.HMSStreamInfo
import com.brytecam.lib.webrtc.HMSRTCMediaStream
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints
import com.brytecam.lib.webrtc.HMSStream
import live.hms.android100ms.R
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.chat.ChatMessage
import live.hms.android100ms.ui.chat.ChatViewModel
import live.hms.android100ms.util.SettingsStore
import live.hms.android100ms.util.viewLifecycle
import org.appspot.apprtc.AppRTCAudioManager
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
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

    private val executor = Executors.newSingleThreadExecutor()

    private var hmsClient: HMSClient? = null
    private var hmsRoom: HMSRoom? = null
    private var localMediaConstraints: HMSRTCMediaStreamConstraints? = null
    private var localMediaStream: HMSRTCMediaStream? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private val chatViewModel: ChatViewModel by navGraphViewModels(R.id.nav_graph) { defaultViewModelProviderFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingBinding.inflate(inflater, container, false)

        settingsStore = SettingsStore(requireContext())
        roomDetails = args.roomDetail

        turnScreenOn()
        initViewModel()
        initRecyclerView()
        initAudioManager()
        init()

        return binding.root
    }

    private fun turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            requireActivity().setTurnScreenOn(true)
        } else {
            requireActivity().window
                .addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                )
        }
    }

    private fun initViewModel() {
        chatViewModel.broadcastMessage.observe(viewLifecycleOwner) { message ->
            hmsClient?.broadcast(message.message, hmsRoom, object : HMSRequestHandler {
                override fun onSuccess(s: String?) {
                    Log.v(TAG, "Successfully broadcast message=${message.message} (s=$s)")
                }

                override fun onFailure(errorCode: Long, errorMessage: String) {
                    Log.v(
                        TAG,
                        "Cannot broadcast message=${message.message} code=${errorCode} errorMessage=${errorMessage}"
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
    private fun init() {

        val perms = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            initHMSClient()
            initFabs()
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
            adapter = MeetingTrackAdapter(meetingTracks)
        }
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

                            currentDeviceTrack = MeetingTrack(null, videoTrack, audioTrack, true)
                            meetingTracks.add(0, currentDeviceTrack!!)

                            requireActivity().runOnUiThread {
                                binding.recyclerView.adapter?.notifyItemInserted(0)
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
        val peer = HMSPeer(roomDetails.username, roomDetails.authToken)
        hmsRoom = HMSRoom(roomDetails.roomId)
        val config = HMSClientConfig(roomDetails.endpoint)
        hmsClient = HMSClient(this, requireContext(), peer, config)
        hmsClient?.apply {
            setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
            connect()
        }
    }

    private fun initFabs() {
        binding.fabEndCall.setOnClickListener { disconnect() }
        binding.fabFlipCamera.setOnClickListener {
            hmsClient?.apply {
                isCameraToggled = true
                switchCamera()
            }
        }

        binding.fabToggleAudio.setOnClickListener {
            currentDeviceTrack?.apply {
                if (audioTrack != null) {
                    isAudioEnabled = !audioTrack.enabled()
                    audioTrack.setEnabled(isAudioEnabled)
                }
            }

            binding.fabToggleAudio.apply {
                val drawable = if (isAudioEnabled)
                    R.drawable.ic_baseline_music_note_24
                else
                    R.drawable.ic_baseline_music_off_24

                this.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        drawable,
                        this@MeetingFragment.requireContext().theme
                    )
                )
            }

        }

        binding.fabToggleVideo.setOnClickListener {
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

            binding.fabToggleVideo.apply {
                val drawable = if (isVideoEnabled)
                    R.drawable.ic_baseline_videocam_24
                else
                    R.drawable.ic_baseline_videocam_off_24

                this.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        drawable,
                        this@MeetingFragment.requireContext().theme
                    )
                )
            }
        }

        binding.fabChat.setOnClickListener {
            findNavController().navigate(
                MeetingFragmentDirections.actionMeetingFragmentToChatFragment(roomDetails)
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
        findNavController().navigate(
            MeetingFragmentDirections.actionMeetingFragmentToHomeFragment()
        )
    }

    // HMS Event Listener events below
    override fun onConnect() {
        shouldReconnect = false
        requireActivity().runOnUiThread {
            // Make Reconnect Progress view invisible
        }

        retryCount = 0
        Log.v(TAG, "Connect success");
        Log.v(
            TAG,
            "You should be able to see local camera feed once the network connection is established and the user is able to join the room"
        );

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
                        binding.recyclerView.adapter?.notifyItemInserted(meetingTracks.size - 1)
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
            if (meetingTracks[i].peer?.uid.equals(streamInfo.uid, true)) {
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
            binding.recyclerView.adapter?.notifyItemRemoved(idx)
        }
    }

    override fun onBroadcast(data: HMSPayloadData) {
        Log.v(TAG, "onBroadcast: peer=${data.peer} senderName=${data.senderName} msg=${data.msg}")
        requireActivity().runOnUiThread {
            // TODO: Remove this once the bug is fixed
            val senderName =
                if (data.senderName == null) "<error:senderName=null>" else data.senderName
            chatViewModel.receivedMessage(
                ChatMessage(
                    senderName,
                    Date(),
                    data.msg
                )
            )
        }
    }
}