package live.hms.android100ms.ui.meeting

import android.app.Application
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.chat.ChatMessage
import live.hms.android100ms.ui.settings.SettingsStore
import live.hms.android100ms.util.*
import live.hms.video.*
import live.hms.video.error.HMSException
import live.hms.video.events.HMSAnalyticsEventLevel
import live.hms.video.network.HMSNetworkQualityInfo
import live.hms.video.payload.HMSPayloadData
import live.hms.video.payload.HMSPublishStream
import live.hms.video.payload.HMSStreamInfo
import live.hms.video.webrtc.HMSRTCAudioTrack
import live.hms.video.webrtc.HMSRTCMediaStreamConstraints
import live.hms.video.webrtc.HMSRTCVideoTrack
import org.json.JSONException
import org.json.JSONObject
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
      crashlytics.setCustomKey(AUTH_TOKEN, authToken)
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

  //plugin realted Data
  var pluginData = MutableLiveData<PluginData?>()

  // TODO: Listen to changes in publishVideo & publishAudio
  //  when it is possible to switch from Audio/Video only to Audio+Video/Audio/Video/etc
  // Live data for user media controls
  val isAudioEnabled = MutableLiveData(settings.publishAudio)
  val isVideoEnabled = MutableLiveData(settings.publishVideo)


  // Live data containing all the current tracks in a meeting
  val tracks = MutableLiveData(_tracks)

  // Live data to notify about broadcast data
  val broadcastsReceived = MutableLiveData<HMSPayloadData>()

  // Dominant speaker
  val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)

  // Network Info
  val networkInfo = MutableLiveData<HMSNetworkQualityInfo>()

  val peer = HMSPeer(roomDetails.username, roomDetails.authToken).apply {
    crashlytics.setUserId(customerUserId)
  }

  private lateinit var localStream: HMSRTCMediaStream

  private val room = HMSRoom(roomDetails.roomId)
  private val config = HMSClientConfig(roomDetails.endpoint).apply {
    hmsAnalyticsEventLevel = HMSAnalyticsEventLevel.INFO
  }
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

  fun logMediaCodecs() {
    val count = MediaCodecList.getCodecCount()
    val arr = ArrayList<MediaCodecInfo>()
    for (i in 0 until count) {
      arr.add(MediaCodecList.getCodecInfoAt(i))
    }

    Log.d(TAG, "logMediaCodecs: $arr")
  }

  fun startMeeting() {
    if (!(state.value is MeetingState.Disconnected || state.value is MeetingState.Failure)) {
      error("Cannot start meeting in ${state.value} state")
    }

    logMediaCodecs()
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
        it.peer.peerId == uid && it.mediaId == mid
      }
      _tracks.remove(trackToRemove)

      // Update the view as we have removed some views
      tracks.postValue(_tracks)
      if (pluginData != null && pluginData.value!!.ownerUid == uid)
        pluginData.postValue(null)
    }
  }

  private fun getConstraintsFromSettings(): HMSRTCMediaStreamConstraints {
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

    crashlyticsLog(TAG, "getConstraintsFromSettings() with $constraintsStr")
    return constraints
  }

  public fun updateLocalMediaStreamConstraints() {
    if (state.value !is MeetingState.Ongoing) {
      throw IllegalStateException(
        "applyConstraints work only in MeetingState.Ongoing " +
            "[Current State: ${state.value}]"
      )
    }

    client.applyConstraints(
      localStream,
      getConstraintsFromSettings(),
      object : HMSClient.LocalStreamListener {
        override fun onSuccess(stream: HMSRTCMediaStream) {
          Toast.makeText(
            getApplication(),
            "Successfully applied new constraints",
            Toast.LENGTH_SHORT
          ).show()
        }

        override fun onFailure(exception: HMSException) = handleFailure(exception)
      })
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
            localStream.streamId,
            peer,
            videoTrack,
            audioTrack,
            true
          ).apply {
            addTrack(this)
            Log.v(TAG, "Adding user track $currentDeviceTrack to VideoGrid")

            if (settings.detectDominantSpeaker) startDetectDominantSpeakerMonitor()
            if (settings.showNetworkInfo) startNetworkInfoMonitor()

            state.postValue(MeetingState.Ongoing())
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

    val constraints = getConstraintsFromSettings()

    state.postValue(
      MeetingState.LoadingMedia(
        "Loading Media",
        "Getting user local stream"
      )
    )

    // onConnect -> Join -> getUserMedia
    client.getLocalStream(
      getApplication(),
      constraints,
      object : HMSClient.LocalStreamListener {
        override fun onSuccess(mediaStream: HMSRTCMediaStream) {
          Log.v(TAG, "GetUserMedia Success")
          localStream = mediaStream
          publishUserStream(constraints)
        }

        override fun onFailure(exception: HMSException) {
          crashlyticsLog(TAG, "GetLocalStream failed: ${toString(exception)}")
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
    /** NOTE: Calling [HMSClient.disconnect] stop's the audio-level & network-info monitor
     * However, we can manually stop it using [HMSClient.stopAudioLevelMonitor]
     * and [HMSClient.stopNetworkMonitor] respectively
     */

    // NOTE: Make sure that we have stopped capturing whenever we disconnect/leave/handle failures
    if (settings.publishVideo) {
      try {
        localStream.cameraVideoCapturer.stop()
      } catch (e: UninitializedPropertyAccessException) {
        e.printStackTrace()
        crashlytics.recordException(e)
      }
    }

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
    crashlytics.recordException(exception)

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
    crashlytics.recordException(exception)

    cleanup()
    state.postValue(MeetingState.Failure(exception))
  }

  override fun onPeerJoin(peer: HMSPeer) {
    crashlyticsLog(
      TAG,
      "onPeerJoin: peerId=${peer.peerId}, " +
          "role=${peer.role}, " +
          "userId=${peer.customerUserId}, " +
          "peerId=${peer.peerId}"
    )
  }

  override fun onPeerLeave(peer: HMSPeer) {
    crashlyticsLog(
      TAG,
      "onPeerLeave: peerId=${peer.peerId}, " +
          "role=${peer.role}, " +
          "userId=${peer.customerUserId}, " +
          "peerId=${peer.peerId}"
    )
  }

  override fun onStreamAdd(peer: HMSPeer, info: HMSStreamInfo) {
    crashlyticsLog(
      TAG,
      "onStreamAdd: peerId:${peer.peerId} " +
          "name=${peer.userName}, " +
          "role=${peer.role} " +
          "userId=${peer.customerUserId} " +
          "mid=${info.mid} " +
          "peerId=${info.peer.peerId}"
    )
    Log.v(TAG, "Adding stream $info ")

    client.subscribe(info, room, object : HMSMediaRequestHandler {
      override fun onSuccess(stream: HMSRTCMediaStream) {
        crashlyticsLog(
          TAG,
          "Subscribe(" +
              "uid=${info.peer.peerId}, " +
              "mid=${info.mid}, " +
              "userName=${info.userName}): " +
              "peer-id=${peer.peerId} -- onSuccess($stream)"
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
          "Subscribe($info): peer-id=${peer.peerId} -- onFailure(${toString(exception)})"
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
          "peerId=${info.peer.peerId} " +
          "mid=${info.mid}"
    )

    removeTrack(info.peer.peerId, info.mid)
  }

  override fun onBroadcast(data: HMSPayloadData) {
    crashlyticsLog(
      TAG,
      "onBroadcast: peerId=${data.peer.peerId}, " +
          "userName=${data.peer.userName}, " +
          "msg=${data.msg}"
    )
    Log.d(TAG, "on broadcast ${data.msg}")
    try {
      // Check if plugin broadcast
      val message = JSONObject(data.msg)
      val type = message.getString("type")
      if (type == "PLUGIN") {
        Log.v(TAG, "Plugin $type")
        val name: String = message.getJSONObject("activePlugin").getString("name")
        val url: String = message.getJSONObject("options").getString("url")
        val isLocked: Boolean = message.getJSONObject("options").optBoolean("isLocked", false)
        val pluginType = when (name) {
          "WHITEBOARD" -> PluginType.WHITEBOARD
          "DRIVE" -> PluginType.DRIVE
          else -> PluginType.WHITEBOARD
        }
        pluginData.postValue(
          PluginData(
            pluginType,
            url,
            data.peer.userName,
            data.peer.peerId,
            isLocked = isLocked
          )
        )
        Log.v(TAG, "Plugin Started ")
      } else if (type == "PLUGIN_CLOSE") {
        pluginData.postValue(null)
        Log.v(TAG, "Plugin closed $pluginData")
      }
    } catch (ex: JSONException) {
      // Otherwise it's a regular chat message
      broadcastsReceived.postValue(data)
    }

  }

  private fun startDetectDominantSpeakerMonitor() {
    client.startAudioLevelMonitor(settings.audioPollInterval) { audioInfo ->
      Log.d(TAG, "startAudioLevelMonitor: $audioInfo")
      // The audioInfo is sorted in descending order of audio-levels
      // 1st item is dominant speaker
      if (audioInfo.isNotEmpty()) {
        if (audioInfo[0].audioLevel > settings.silenceAudioLevelThreshold) {
          synchronized(_tracks) {
            _tracks.find { it.mediaId == audioInfo[0].streamId }?.let {
              dominantSpeaker.postValue(it)
            }
          }
        } else dominantSpeaker.postValue(null)
      }
    }
  }

  private fun startNetworkInfoMonitor() {
    client.startNetworkMonitor { networkInfo.postValue(it[0]) }
  }
}
