package live.hms.android100ms.ui.meeting

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.brytecam.lib.*
import com.brytecam.lib.payload.HMSPayloadData
import com.brytecam.lib.payload.HMSPublishStream
import com.brytecam.lib.payload.HMSStreamInfo
import com.brytecam.lib.webrtc.HMSRTCMediaStream
import com.brytecam.lib.webrtc.HMSRTCMediaStreamConstraints
import com.brytecam.lib.webrtc.HMSStream
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.chat.ChatMessage
import live.hms.android100ms.util.*
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.VideoTrack

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

  private val _tracks = ArrayList<MeetingTrack>()
  private var currentDeviceTrack: MeetingTrack? = null

  // Flag to keep track whether the incoming audio need's to be muted
  private var _isAudioMuted = false

  // Public variable which can be accessed by views
  public val isAudioMuted: Boolean
    get() = _isAudioMuted

  private val settings = SettingsStore(getApplication())

  // Live data to define the overall UI
  val state = MutableLiveData<MeetingState>(MeetingState.Disconnected(false))

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

  private val room = HMSRoom(roomDetails.roomId)
  private val config = HMSClientConfig(roomDetails.endpoint)
  private var client = HMSClient(this, getApplication(), peer, config).apply {
    setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
  }

  fun toggleUserMic() {
    currentDeviceTrack?.videoTrack?.apply {
      val isVideo = !enabled()
      setEnabled(isVideo)
      if (isVideo) {
        HMSStream.getCameraCapturer().start()
      } else {
        HMSStream.getCameraCapturer().stop()
      }

      isVideoEnabled.postValue(isVideo)
    }
  }

  fun toggleUserVideo() {
    currentDeviceTrack?.audioTrack?.apply {
      val isAudio = !enabled()
      setEnabled(isAudio)

      isAudioEnabled.postValue(isAudio)
    }
  }

  /**
   * Helper function to toggle others audio tracks
   */
  fun toggleAudio() {
    _isAudioMuted = !_isAudioMuted

    val volume = if (_isAudioMuted) 0.0 else 1.0
    _tracks.forEach { track ->
      if (track != currentDeviceTrack) {
        track.audioTrack?.setVolume(volume)
      }
    }
  }

  fun startMeeting() {
    if (state.value !is MeetingState.Disconnected) {
      error("Cannot start meeting in ${state.value} state")
    }

    state.postValue(
      MeetingState.Connecting(
        "Connecting",
        "Please wait while we connect you to ${roomDetails.endpoint}"
      )
    )

    // FIXME(HMS-1115): Using the same HMSClient instance to call connect() after
    //  failure raises the same error.
    client = HMSClient(this, getApplication(), peer, config).apply {
      setLogLevel(HMSLogger.LogLevel.LOG_DEBUG)
    }

    client.connect()
  }

  fun flipCamera() {
    client.switchCamera()
  }

  fun leaveMeeting() {
    state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
    cleanup()

    client.leave(object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        crashlyticsLog(TAG, "[${Thread.currentThread()}] hmsClient.leave() -> onSuccess($data)")
        client.disconnect()
        state.postValue(MeetingState.Disconnected(goToHome = true))
      }

      override fun onFailure(code: Long, reason: String) {
        crashlyticsLog(TAG, "hmsClient.leave() -> onFailure($code, $reason)")
        state.postValue(MeetingState.Disconnected(true, "[$code] Leave Failure", reason, true))
      }
    })
  }

  fun broadcastMessage(message: ChatMessage) {
    Log.d(TAG, "Sending broadcast $message via $client")
    client.broadcast(message.message, room, object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        Log.v(TAG, "Successfully broadcast message=${message.message} onSuccess($data)")
      }

      override fun onFailure(code: Long, reason: String) {
        Toast.makeText(
          getApplication(),
          "Cannot send '${message}'. Please try again",
          Toast.LENGTH_SHORT
        ).show()
        crashlyticsLog(TAG, "Cannot broadcast message=${message} code=${code} reason=${reason}")
      }
    })
  }

  private fun addTrack(track: MeetingTrack) {
    if (track.isCurrentDeviceStream) {
      _tracks.add(0, track)
    } else {
      _tracks.add(track)
    }

    tracks.postValue(_tracks)
  }

  private fun removeTrack(uid: String, mid: String) {
    var found = false
    val toRemove = ArrayList<MeetingTrack>()

    _tracks.forEach { track ->
      if (track.peer.uid == uid && track.mediaId == mid) {
        found = true
        toRemove.add(track)
      }
    }

    _tracks.removeAll(toRemove)
    if (!found) {
      crashlyticsLog(TAG, "onStreamRemove: $uid & $mid not found in meeting tracks")
    } else {
      // Update the grid layout as we have removed some views
      tracks.postValue(_tracks)
    }
  }

  private fun publishUserStream(
    constraints: HMSRTCMediaStreamConstraints,
    mediaStream: HMSRTCMediaStream
  ) {
    state.postValue(
      MeetingState.PublishingMedia(
        "Publishing Media",
        "Publishing user audio & video"
      )
    )

    var videoTrack: VideoTrack? = null
    var audioTrack: AudioTrack? = null

    mediaStream.stream?.apply {

      if (videoTracks.isNotEmpty()) {
        videoTrack = videoTracks[0]
        videoTrack?.setEnabled(settings.publishVideo)
      }
      if (audioTracks.isNotEmpty()) {
        audioTrack = audioTracks[0]
        audioTrack?.setEnabled(settings.publishAudio)
      }
    }

    client.publish(
      mediaStream,
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

        override fun onFailure(code: Long, reason: String) {
          crashlyticsLog(TAG, "Publish Failure $code $reason")
          handleFailure(false, "[$code] Publish Failure", reason)
        }
      })
  }

  private fun getUserMedia() {
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
    client.getUserMedia(
      getApplication(),
      constraints,
      object : HMSClient.GetUserMediaListener {
        override fun onSuccess(mediaStream: HMSRTCMediaStream) {
          Log.v(TAG, "GetUserMedia Success")
          publishUserStream(constraints, mediaStream)
        }

        override fun onFailure(code: Long, reason: String) {
          crashlyticsLog(TAG, "GetUserMedia failed: $code $reason")
          handleFailure(false, "[$code] GetUserMedia Failure", reason)
        }
      })

  }

  private fun joinMeeting() {
    state.postValue(MeetingState.Joining("Joining Room", "Connected! Joining meeting.."))

    client.join(object : HMSRequestHandler {
      override fun onSuccess(data: String) {
        crashlyticsLog(TAG, "Join onSuccess($data)")
        // TODO: Start audio-manager
        getUserMedia()
      }

      override fun onFailure(code: Long, reason: String) {
        crashlyticsLog(TAG, "Join onFailure($code, $reason)")
        handleFailure(false, "[$code] Join Failure", reason)
      }
    })
  }

  /**
   * Called whenever the meeting is ended (both due to user action or failure)
   * Resets all the values to default
   */
  private fun cleanup() {
    // NOTE: Make sure that we have stopped capturing whenever we disconnect/leave/handle failures
    HMSStream.stopCapturers()

    // Reset the values of bottom control buttons
    isAudioEnabled.postValue(settings.publishAudio)
    isVideoEnabled.postValue(settings.publishVideo)

    _isAudioMuted = false

    // Remove all the video stream
    _tracks.clear()
    tracks.postValue(_tracks)

    crashlyticsLog(TAG, "cleanup() done")
  }

  /**
   * @param fatal Failure requires closing the MeetingActivity,
   *  going back to home page
   * @param title Set the title displayed in the Dialog
   * @param message Set the message to display.
   */
  private fun handleFailure(
    fatal: Boolean,
    title: String,
    message: String
  ) {
    crashlyticsLog(TAG, "handleFailure($fatal, $title, $message)")

    client.disconnect()
    cleanup()
    state.postValue(MeetingState.Disconnected(true, title, message, fatal))
  }

  // HMS Events
  override fun onConnect() {
    Log.d(TAG, "onConnect()")
    joinMeeting()
  }

  override fun onDisconnect(errorMessage: String) {
    crashlyticsLog(TAG, "onDisconnect: $errorMessage")
    cleanup()
    state.postValue(MeetingState.Disconnected(true, "Disconnected", errorMessage))
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
      override fun onSuccess(stream: MediaStream) {
        crashlyticsLog(
          TAG,
          "Subscribe(" +
              "uid=${info.uid}, " +
              "mid=${info.mid}, " +
              "userName=${info.userName}): " +
              "peer-id=${peer.uid} -- onSuccess($stream)"
        )

        var videoTrack: VideoTrack? = null
        var audioTrack: AudioTrack? = null

        if (stream.videoTracks.size > 0) {
          videoTrack = stream.videoTracks[0]
          videoTrack.setEnabled(true)
        }

        if (stream.audioTracks.size > 0) {
          audioTrack = stream.audioTracks[0]
          audioTrack.setEnabled(true)

          if (_isAudioMuted) {
            audioTrack.setVolume(0.0)
          } else {
            audioTrack.setVolume(1.0)
          }
        }

        addTrack(MeetingTrack(info.mid, peer, videoTrack, audioTrack, false))
      }

      override fun onFailure(code: Long, reason: String) {
        crashlyticsLog(TAG, "Subscribe($info): peer-id=${peer.uid} -- onFailure($code, $reason)")
        handleFailure(true, "[$code] Subscribe Failure", reason)
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
