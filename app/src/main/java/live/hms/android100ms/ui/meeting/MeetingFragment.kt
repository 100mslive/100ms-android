package live.hms.android100ms.ui.meeting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.brytecam.lib.*
import com.brytecam.lib.payload.HMSPayloadData
import com.brytecam.lib.payload.HMSPublishStream
import com.brytecam.lib.payload.HMSStreamInfo
import com.brytecam.lib.webrtc.HMSRTCMediaStream
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints
import com.brytecam.lib.webrtc.HMSStream
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.android100ms.R
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.home.HomeActivity
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.chat.ChatMessage
import live.hms.android100ms.ui.meeting.chat.ChatViewModel
import live.hms.android100ms.ui.meeting.videogrid.VideoGridAdapter
import live.hms.android100ms.util.ROOM_DETAILS
import live.hms.android100ms.util.viewLifecycle
import org.appspot.apprtc.AppRTCAudioManager
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.VideoTrack
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class MeetingFragment : Fragment(), HMSEventListener {

  companion object {
    private const val TAG = "MeetingFragment"

    private const val FRONT_FACING_CAMERA = "user"
    private const val REAR_FACING_CAMERA = "environment"
  }

  private var binding by viewLifecycle<FragmentMeetingBinding>()

  private lateinit var settings: SettingsStore
  private lateinit var roomDetails: RoomDetails

  private var isJoined = false
  private var isPublished = false

  private var isFrontCameraEnabled = true
  private var isCameraToggled = false
  private var isAudioEnabled = true
  private var isVideoEnabled = true

  private var currentDeviceTrack: MeetingTrack? = null
  private val videoGridItems = ArrayList<MeetingTrack>()

  private val executor = Executors.newSingleThreadExecutor()

  private var hmsClient: HMSClient? = null
  private var hmsRoom: HMSRoom? = null
  private var hmsPeer: HMSPeer? = null
  private var localMediaConstraints: HMSRTCMediaStreamConstraints? = null
  private var localMediaStream: HMSRTCMediaStream? = null
  private var localAudioTrack: AudioTrack? = null
  private var localVideoTrack: VideoTrack? = null

  private val chatViewModel: ChatViewModel by activityViewModels()

  private lateinit var clipboard: ClipboardManager

  private lateinit var audioManager: AppRTCAudioManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    clipboard = requireActivity()
      .getSystemService(Context.CLIPBOARD_SERVICE)
        as ClipboardManager


    initAudioManager()
    initHMSClient()
  }

  override fun onDestroy() {
    super.onDestroy()
    destructAudioManager()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_share_link -> {
        val meetingUrl = roomDetails.let {
          "https://${it.env}.100ms.live/?room=${it.roomId}&env=${it.env}&role=Guest"
        }
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, meetingUrl)
          type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
      }
      R.id.action_record_meeting -> {
        Toast.makeText(requireContext(), "Recording Not Supported", Toast.LENGTH_SHORT).show()
      }
      R.id.action_share_screen -> {
        Toast.makeText(requireContext(), "Screen Share Not Supported", Toast.LENGTH_SHORT).show()
      }
    }
    return false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViewModel()
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentMeetingBinding.inflate(inflater, container, false)

    settings = SettingsStore(requireContext())

    initVideoGrid()
    initButtons()
    initOnBackPress()

    return binding.root
  }

  private fun initViewModel() {
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
    audioManager = AppRTCAudioManager.create(requireContext())
    Log.d(TAG, "Starting Audio manager")

    audioManager.start { selectedAudioDevice, availableAudioDevices ->
      Log.d(
        TAG,
        "onAudioManagerDevicesChanged: $availableAudioDevices, selected: $selectedAudioDevice"
      )
    }
  }

  private fun destructAudioManager() {
    audioManager.stop()
  }

  private fun initVideoGrid() {
    binding.viewPagerVideoGrid.apply {
      adapter = VideoGridAdapter(this@MeetingFragment) { video ->
        // TODO: Implement Hero/Pin View

        Log.v(TAG, "onVideoItemClick: $video")

        Snackbar.make(
          binding.root,
          "Name: ${video.peer.userName} (${video.peer.role}) \nId: ${video.peer.customerUserId}",
          Snackbar.LENGTH_LONG,
        ).setAction("Copy") {
          val clip = ClipData.newPlainText("Customer Id", video.peer.customerUserId)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(
            requireContext(),
            "Copied customer id of ${video.peer.userName} to clipboard",
            Toast.LENGTH_SHORT
          ).show()
        }.show()
      }

      TabLayoutMediator(binding.tabLayoutDots, this) { _, _ ->
        // No text to be shown
      }.attach()
    }

  }

  private fun updateVideoGridUI() {
    val adapter = binding.viewPagerVideoGrid.adapter as VideoGridAdapter
    adapter.setItems(videoGridItems)
  }

  private fun getUserMedia(
    frontCamEnabled: Boolean,
    audioEnabled: Boolean,
    cameraToggle: Boolean
  ) {
    localMediaConstraints = HMSRTCMediaStreamConstraints(true, settings.publishVideo)
    localMediaConstraints?.apply {
      videoCodec = settings.codec
      videoFrameRate = settings.videoFrameRate
      videoResolution = settings.videoResolution
      videoMaxBitRate = settings.videoBitrate

      if (frontCamEnabled) {
        isFrontCameraEnabled = true
        cameraFacing = FRONT_FACING_CAMERA
      } else {
        cameraFacing = REAR_FACING_CAMERA
      }
    }

    hmsClient?.getUserMedia(
      requireContext(),
      localMediaConstraints,
      object : HMSClient.GetUserMediaListener {
        override fun onSuccess(mediaStream: HMSRTCMediaStream?) {
          Log.v(TAG, "GetUserMedia Success")
          localMediaStream = mediaStream

          var videoTrack: VideoTrack? = null
          var audioTrack: AudioTrack? = null

          mediaStream?.stream?.apply {
            if (videoTracks.isNotEmpty()) {
              videoTrack = videoTracks[0]
              videoTrack?.setEnabled(settings.publishVideo)
            }
            if (audioTracks.isNotEmpty()) {
              audioTrack = audioTracks[0]
              audioTrack?.setEnabled(settings.publishAudio)
            }

            currentDeviceTrack = MeetingTrack(
              hmsPeer!!,
              videoTrack,
              audioTrack,
              true
            )

            requireActivity().runOnUiThread {
              Log.v(TAG, "Adding $currentDeviceTrack to ViewPagerVideoGrid")
              videoGridItems.add(0, currentDeviceTrack!!)
              updateVideoGridUI()
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

  private fun initHMSClient() {
    hmsPeer = HMSPeer(roomDetails.username, roomDetails.authToken)
    hmsRoom = HMSRoom(roomDetails.roomId)
    val config = HMSClientConfig(roomDetails.endpoint)
    hmsClient = HMSClient(this, requireContext(), hmsPeer, config).apply {
      setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
      connect()
    }
  }

  /**
   * Changes the icons for buttons as per the current settings
   */
  private fun updateButtonsUI() {
    binding.buttonToggleAudio.apply {
      setIconResource(
        if (isAudioEnabled)
          R.drawable.ic_baseline_music_note_24
        else
          R.drawable.ic_baseline_music_off_24
      )
    }

    binding.buttonToggleVideo.apply {
      setIconResource(
        if (isVideoEnabled)
          R.drawable.ic_baseline_videocam_24
        else
          R.drawable.ic_baseline_videocam_off_24
      )
    }
  }

  private fun initButtons() {
    updateButtonsUI()

    binding.buttonToggleVideo.setOnClickListener {
      currentDeviceTrack?.apply {
        if (videoTrack != null) {
          isVideoEnabled = !videoTrack.enabled()
          videoTrack.setEnabled(isVideoEnabled)
          if (isVideoEnabled) {
            HMSStream.getCameraCapturer().start()
          } else {
            HMSStream.getCameraCapturer().stop()
          }
          updateButtonsUI()
        }
      }
    }

    binding.buttonToggleAudio.setOnClickListener {
      currentDeviceTrack?.apply {
        if (audioTrack != null) {
          isAudioEnabled = !audioTrack.enabled()
          audioTrack.setEnabled(isAudioEnabled)
          updateButtonsUI()
        }
      }
    }

    binding.buttonOpenChat.setOnClickListener {
      findNavController().navigate(
        MeetingFragmentDirections.actionMeetingFragmentToChatBottomSheetFragment(
          roomDetails,
          hmsPeer!!.customerUserId
        )
      )
    }

    binding.buttonEndCall.setOnClickListener { disconnect() }

    binding.buttonFlipCamera.setOnClickListener {
      hmsClient?.apply {
        isCameraToggled = true
        switchCamera()
      }
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

    // Go to home page
    Intent(requireContext(), HomeActivity::class.java).apply {
      startActivity(this)
      requireActivity().finish()
    }
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
    // TODO: Make Reconnect Progress view invisible

    Log.v(TAG, "onConnect");

    if (!isJoined) {
      hmsClient?.join(object : HMSRequestHandler {
        override fun onSuccess(p0: String?) {
          isJoined = true
          Log.v(TAG, "Join Success")
          Thread.sleep(1000)
          getUserMedia(
            isFrontCameraEnabled,
            settings.publishAudio,
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
    isJoined = false
    isPublished = false

    // TODO: Clean up of views
    // TODO: Handle no internet available / lost internet / network change

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

          requireActivity().runOnUiThread {
            videoGridItems.add(MeetingTrack(peer, videoTrack, audioTrack))
            updateVideoGridUI()
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
    for (i in 0..videoGridItems.size step 1) {
      if (videoGridItems[i].peer.uid.equals(streamInfo.uid, true)) {
        idx = i
        break
      }
    }

    if (idx == -1) {
      Log.v(TAG, "onStreamRemove: ${streamInfo.uid} not found in meeting tracks")
      return
    }

    requireActivity().runOnUiThread {
      videoGridItems.removeAt(idx)
      updateVideoGridUI()
    }
  }

  override fun onBroadcast(data: HMSPayloadData) {
    Log.v(
      TAG,
      "onBroadcast: customerId=${data.peer.customerUserId} userName=${data.peer.userName} msg=${data.msg}"
    )
    requireActivity().runOnUiThread {
      // TODO
      var username  = data.peer.userName
      if (username.isEmpty()) username = "error<senderName=null>"

      chatViewModel.receivedMessage(
        ChatMessage(
          // TODO: Change uid -> Customer ID
          data.peer.uid,
          username,
          Date(),
          data.msg,
          false
        )
      )
    }
  }
}