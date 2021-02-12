package live.hms.android100ms.ui.meeting

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.chat.ChatMessage
import live.hms.android100ms.util.*
import live.hms.video.*
import live.hms.video.error.HMSException
import live.hms.video.payload.HMSPayloadData
import live.hms.video.payload.HMSPublishStream
import live.hms.video.payload.HMSStreamInfo
import live.hms.video.webrtc.HMSRTCAudioTrack
import live.hms.video.webrtc.HMSRTCMediaStreamConstraints
import live.hms.video.webrtc.HMSRTCVideoTrack
import java.util.*
import kotlin.collections.ArrayList

class MeetingViewModel(
  application: Application,
  private val roomDetails: RoomDetails
) : AndroidViewModel(application), HMSEventListener {
  companion object {
    private const val TAG = "MeetingViewModel"
  }

  init {
    roomDetails.apply {
      crashlytics.setCustomKey(ROOM_ID, roomId)
      crashlytics.setCustomKey(USERNAME, username)
      crashlytics.setCustomKey(ROOM_ENDPOINT, endpoint)
    }
  }

  private val _tracks = Collections.synchronizedList(ArrayList<MeetingTrack>())
  private var currentDeviceTrack: MeetingTrack? = null

  // Flag to keep track whether the incoming audio need's to be muted
  private var _isAudioMuted = false

  // Public variable which can be accessed by views
  val isAudioMuted: Boolean
    get() = _isAudioMuted

  private val settings = SettingsStore(getApplication())

  // Live data to define the overall UI
  val state = MutableLiveData<MeetingState>(MeetingState.Disconnected())

  // TODO: Listen to changes in publishVideo & publishAudio
  //  when it is possible to switch from Audio/Video only to Audio+Video/Audio/Video/etc
  // Live data for user media controls
  val isAudioEnabled = MutableLiveData(settings.publishAudio)
  val isVideoEnabled = MutableLiveData(settings.publishVideo)


  // Live data containing all the current tracks in a meeting
  val tracks = MutableLiveData(_tracks)

  // Live data to notify about broadcast data
  val broadcastsReceived = MutableLiveData<HMSPayloadData>()

  val peer = HMSPeer(roomDetails.username, roomDetails.authToken).apply {
    crashlytics.setUserId(customerUserId)
  }

  private lateinit var localStream: HMSRTCMediaStream

  private val room = HMSRoom(roomDetails.roomId)
  private val config = HMSClientConfig(roomDetails.endpoint)
  private val client = HMSClient(this, getApplication(), peer, config).apply {
    setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
  }

  fun toggleUserVideo() {
    currentDeviceTrack?.videoTrack?.apply {
      val isVideo = !enabled
      enabled = isVideo
      if (isVideo) {
        localStream.cameraVideoCapturer.start()
      } else {
        localStream.cameraVideoCapturer.stop()
      }

      isVideoEnabled.postValue(isVideo)
      crashlyticsLog(TAG, "toggleUserVideo: enabled=$isVideo")
    }
  }

  fun toggleUserMic() {
    currentDeviceTrack?.audioTrack?.apply {
      val isAudio = !enabled
      enabled = isAudio

      isAudioEnabled.postValue(isAudio)
      crashlyticsLog(TAG, "toggleUserMic: enabled=$isAudio")
    }
  }

  /**
   * Helper function to toggle others audio tracks
   */
  fun toggleAudio() {
    synchronized(_tracks) {
      _isAudioMuted = !_isAudioMuted

      val volume = if (_isAudioMuted) 0.0 else 1.0
      _tracks.forEach { track ->
        if (track != currentDeviceTrack) {
          track.audioTrack?.setVolume(volume)
        }
      }
    }
  }

  fun startMeeting() {
    if (!(state.value is MeetingState.Disconnected || state.value is MeetingState.Failure)) {
      error("Cannot start meeting in ${state.value} state")
    }

    state.postValue(
      MeetingState.Connecting(
        "Connecting",
        "Please wait while we connect you to ${roomDetails.endpoint}"
      )
    )

    client.connect()
  }

  fun flipCamera() {
    if (!settings.publishVideo) {
      error("Cannot switch camera when Video is disabled")
    }

    // NOTE: During audio-only calls, this switch-camera is ignored
    //  as no camera in use
    localStream.cameraVideoCapturer.switchCamera()
  }

  fun leaveMeeting() {
    state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
    cleanup()

    client.leave(object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        crashlyticsLog(TAG, "[${Thread.currentThread()}] hmsClient.leave() -> onSuccess($data)")
        client.disconnect()
        state.postValue(MeetingState.Disconnected(true))
      }

      override fun onFailure(exception: HMSException) {
        crashlyticsLog(TAG, "hmsClient.leave() -> onFailure(${toString(exception)}")
        state.postValue(MeetingState.Failure(exception))
      }
    })
  }

  fun broadcastMessage(message: ChatMessage) {
    Log.d(TAG, "Sending broadcast $message via $client")
    client.broadcast(message.message, room, object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        Log.v(TAG, "Successfully broadcast message=${message.message} onSuccess($data)")
      }

      override fun onFailure(exception: HMSException) {
        Toast.makeText(
          getApplication(),
          "Cannot send '${message}'. Please try again",
          Toast.LENGTH_SHORT
        ).show()
        crashlyticsLog(TAG, "Cannot broadcast message=${message} onFailure(${toString(exception)}")
      }
    })
  }

  private fun addTrack(track: MeetingTrack) {
    synchronized(_tracks) {
      if (track.isCurrentDeviceStream) {
        _tracks.add(0, track)
      } else {
        _tracks.add(track)
      }

      tracks.postValue(_tracks)
    }
  }

  private fun removeTrack(uid: String, mid: String) {
    synchronized(_tracks) {
      val trackToRemove = _tracks.find {
        it.peer.uid == uid && it.mediaId == mid
      }
      _tracks.remove(trackToRemove)

      // Update the view as we have removed some views
      tracks.postValue(_tracks)
    }
  }

  private fun publishUserStream(constraints: HMSRTCMediaStreamConstraints) {
    state.postValue(
      MeetingState.PublishingMedia(
        "Publishing Media",
        "Publishing user audio & video"
      )
    )

    var videoTrack: HMSRTCVideoTrack? = null
    var audioTrack: HMSRTCAudioTrack? = null

    localStream.videoTracks.let { tracks ->
      if (tracks.isNotEmpty()) {
        videoTrack = tracks[0]
      }
    }

    localStream.audioTracks.let { tracks ->
      if (tracks.isNotEmpty()) {
        audioTrack = tracks[0]
      }
    }

    client.publish(
      localStream,
      room,
      constraints,
      object : HMSStreamRequestHandler {
        override fun onSuccess(data: HMSPublishStream) {
          crashlyticsLog(TAG, "Publish Success ${data.mid}")

          currentDeviceTrack = MeetingTrack(
            data.mid,
            peer,
            videoTrack,
            audioTrack,
            true
          ).apply {
            state.postValue(MeetingState.Ongoing())

            addTrack(this)
            Log.v(TAG, "Adding user track $currentDeviceTrack to VideoGrid")
          }
        }

        override fun onFailure(exception: HMSException) {
          crashlyticsLog(TAG, "Publish Failure onFailure(${toString(exception)})")
          handleFailure(exception)
        }
      })
  }

  private fun getLocalScreen() {
    // TODO: Listen to changes in settings.publishVideo
    //  To be done only when the user can change the publishVideo
    //  while in a meeting.

    val constraints = HMSRTCMediaStreamConstraints(settings.publishAudio, settings.publishVideo)

    val resolution = "${settings.videoResolutionWidth}" +
        "x${settings.videoResolutionHeight}" +
        "@${settings.videoFrameRate}"

    constraints.apply {
      videoCodec = settings.codec
      videoFrameRate = settings.videoFrameRate
      videoResolution = resolution
      videoMaxBitRate = settings.videoBitrate
      cameraFacing = settings.camera
    }

    val constraintsStr = "videoCodec=${constraints.videoCodec}, " +
        "videoFrameRate=${constraints.videoFrameRate}, " +
        "videoResolution=${resolution}, " +
        "videoMaxBitRate=${constraints.videoMaxBitRate}, " +
        "cameraFacing=${constraints.cameraFacing}, "

    crashlyticsLog(TAG, "getUserMedia() with $constraintsStr")

    state.postValue(
      MeetingState.LoadingMedia(
        "Loading Media",
        "Getting user audio & video with $constraintsStr"
      )
    )

    // onConnect -> Join -> getUserMedia
    client.getLocalStream(
      getApplication(),
      constraints,
      object : HMSClient.GetLocalStreamListener {
        override fun onSuccess(mediaStream: HMSRTCMediaStream) {
          Log.v(TAG, "GetUserMedia Success")
          localStream = mediaStream
          publishUserStream(constraints)
        }

        override fun onFailure(exception: HMSException) {
          crashlyticsLog(TAG, "GetUserMedia failed: ${toString(exception)}")
          handleFailure(exception)
        }
      })

  }

  private fun joinMeeting() {
    state.postValue(MeetingState.Joining("Joining Room", "Connected! Joining meeting.."))

    client.join(object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        crashlyticsLog(TAG, "Join onSuccess($data)")
        // TODO: Start audio-manager
        getLocalScreen()
      }

      override fun onFailure(exception: HMSException) {
        crashlyticsLog(TAG, "Join onFailure(${toString(exception)})")
        handleFailure(exception)
      }
    })
  }

  /**
   * Called whenever the meeting is ended (both due to user action or failure)
   * Resets all the values to default
   */
  private fun cleanup() {
    // NOTE: Make sure that we have stopped capturing whenever we disconnect/leave/handle failures
    localStream.cameraVideoCapturer.stop()
    // TODO: Check if audio & video should be disposed

    // Reset the values of bottom control buttons
    isAudioEnabled.postValue(settings.publishAudio)
    isVideoEnabled.postValue(settings.publishVideo)

    _isAudioMuted = false

    // Remove all the video stream
    synchronized(_tracks) {
      _tracks.clear()
      tracks.postValue(_tracks)
    }

    crashlyticsLog(TAG, "cleanup() done")
  }

  /**
   * @param exception [HMSException] Failure instance
   */
  private fun handleFailure(exception: HMSException) {
    crashlyticsLog(TAG, "handleFailure(${toString(exception)})")

    client.disconnect()
    cleanup()
    state.postValue(MeetingState.Failure(exception))
  }

  // HMS Events
  override fun onConnect() {
    Log.d(TAG, "onConnect()")
    joinMeeting()
  }

  override fun onDisconnect(exception: HMSException) {
    crashlyticsLog(TAG, "onDisconnect: ${toString(exception)}")
    cleanup()
    state.postValue(MeetingState.Failure(exception))
  }

  override fun onPeerJoin(peer: HMSPeer) {
    crashlyticsLog(
      TAG,
      "onPeerJoin: uid=${peer.uid}, " +
          "role=${peer.role}, " +
          "userId=${peer.customerUserId}, " +
          "peerId=${peer.peerId}"
    )
  }

  override fun onPeerLeave(peer: HMSPeer) {
    crashlyticsLog(
      TAG,
      "onPeerLeave: uid=${peer.uid}, " +
          "role=${peer.role}, " +
          "userId=${peer.customerUserId}, " +
          "peerId=${peer.peerId}"
    )
  }

  override fun onStreamAdd(peer: HMSPeer, info: HMSStreamInfo) {
    crashlyticsLog(
      TAG,
      "onStreamAdd: peer-uid:${peer.uid} " +
          "name=${peer.userName}, " +
          "role=${peer.role} " +
          "userId=${peer.customerUserId} " +
          "mid=${info.mid} " +
          "uid=${info.uid}"
    )

    client.subscribe(info, room, object : HMSMediaRequestHandler {
      override fun onSuccess(stream: HMSRTCMediaStream) {
        crashlyticsLog(
          TAG,
          "Subscribe(" +
              "uid=${info.uid}, " +
              "mid=${info.mid}, " +
              "userName=${info.userName}): " +
              "peer-id=${peer.uid} -- onSuccess($stream)"
        )

        var videoTrack: HMSRTCVideoTrack? = null
        var audioTrack: HMSRTCAudioTrack? = null

        // Video and Audio tracks are enabled by default
        if (stream.videoTracks.size > 0) {
          videoTrack = stream.videoTracks[0]
        }

        if (stream.audioTracks.size > 0) {
          audioTrack = stream.audioTracks[0]

          if (_isAudioMuted) {
            audioTrack.setVolume(0.0)
          } else {
            audioTrack.setVolume(1.0)
          }
        }

        addTrack(MeetingTrack(info.mid, peer, videoTrack, audioTrack, false, info.isScreen))
      }

      override fun onFailure(exception: HMSException) {
        crashlyticsLog(
          TAG,
          "Subscribe($info): peer-id=${peer.uid} -- onFailure(${toString(exception)})"
        )
        handleFailure(exception)
      }
    })
  }

  override fun onStreamRemove(info: HMSStreamInfo) {
    crashlyticsLog(
      TAG,
      "onStreamRemove: " +
          "name=${info.userName} " +
          "uid=${info.uid} " +
          "mid=${info.mid}"
    )

    removeTrack(info.uid, info.mid)
  }

  override fun onBroadcast(data: HMSPayloadData) {
    crashlyticsLog(
      TAG,
      "onBroadcast: customerId=${data.peer.customerUserId}, " +
          "userName=${data.peer.userName}, " +
          "msg=${data.msg}"
    )

    broadcastsReceived.postValue(data)
  }
}
