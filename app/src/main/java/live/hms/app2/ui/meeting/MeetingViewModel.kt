package live.hms.app2.ui.meeting

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.*
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.HMSUpdateListener
import live.hms.video.sdk.models.HMSConfig
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.utils.*
import java.util.*
import kotlin.collections.ArrayList

class MeetingViewModel(
  application: Application,
  private val roomDetails: RoomDetails
) : AndroidViewModel(application) {
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

  private val _videoTracks = Collections.synchronizedList(ArrayList<MeetingTrack>())
  private val _audioTracks = Collections.synchronizedList(ArrayList<MeetingTrack>())

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

  private var localAudioTrack: HMSLocalAudioTrack? = null
  private var localVideoTrack: HMSLocalVideoTrack? = null

  // Live data containing all the current tracks in a meeting
  val videoTracks = MutableLiveData(_videoTracks)
  val audioTracks = MutableLiveData(_audioTracks)

  // Dominant speaker
  val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)

  val broadcastsReceived = MutableLiveData<String>()

  private val hmsSDK = HMSSDK
    .Builder(application)
    .setLogLevel(HMSLogger.LogLevel.VERBOSE)
    .build()

  fun toggleUserVideo() {
    HMSCoroutineScope.launch {
      localVideoTrack?.apply {
        val isVideo = !isEnabled
        setEnabled(isVideo)
        if (isVideo) {
          startCapturing()
        } else {
          stopCapturing()
        }

        isVideoEnabled.postValue(isVideo)
        crashlyticsLog(TAG, "toggleUserVideo: enabled=$isVideo")
      }
    }
  }

  fun toggleUserMic() {
    HMSCoroutineScope.launch {
      localAudioTrack?.apply {
        val isAudio = !isEnabled
        setEnabled(isAudio)

        isAudioEnabled.postValue(isAudio)
        crashlyticsLog(TAG, "toggleUserMic: enabled=$isAudio")
      }
    }
  }

  /**
   * Helper function to toggle others audio tracks
   */
  fun toggleAudio() {
    synchronized(_videoTracks) {
      _isAudioMuted = !_isAudioMuted

      val volume = if (_isAudioMuted) 0.0 else 1.0
      _audioTracks.forEach { track ->
        if (track.audio != null && track.audio != localAudioTrack) {
          (track.audio as HMSRemoteAudioTrack).setVolume(volume)
        }
      }
    }
  }

  fun makeToken(): String {
    val token = AuthTokenUtils.AuthToken(
      roomDetails.roomId,
      IdHelper.makePeerId() + roomDetails.username,
      "Guest"
    )
    val tokenStr = GsonUtils.gson.toJson(token)
    val base64Payload = Base64.encodeToString(tokenStr.toByteArray(), Base64.DEFAULT)
    return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.${base64Payload}.7WJIaNM6KZqGLqw5ESocQMGIx3b_ckWyu1FNW27gL5E"
  }

  fun startMeeting() {
    if (!(state.value is MeetingState.Disconnected || state.value is MeetingState.Failure)) {
      error("Cannot start meeting in ${state.value} state")
    }

    state.postValue(
      MeetingState.Connecting(
        "Connecting",
        "Establishing websocket connection"
      )
    )

    HMSCoroutineScope.launch {
      val info = JsonObject().apply { addProperty("name", roomDetails.username) }
      val config = HMSConfig(roomDetails.username, roomDetails.authToken, info.toString())
      hmsSDK.join(config, object : HMSUpdateListener {
        override fun onError(error: HMSException) {
          Log.e(TAG, error.toString())
          state.postValue(MeetingState.Failure(error))
        }

        override fun onJoin(hmsRoom: HMSRoom) {
          val peer = hmsSDK.getLocalPeer()
          peer.audioTrack?.apply {
            localAudioTrack = (this as HMSLocalAudioTrack)
            addTrack(MeetingTrack(trackId, peer.name, null, localAudioTrack, true, false))
          }
          peer.videoTrack?.apply {
            localVideoTrack = (this as HMSLocalVideoTrack)
            localVideoTrack!!.startCapturing()
            addTrack(MeetingTrack(trackId, peer.name, localVideoTrack, null, true, false))
          }
          state.postValue(MeetingState.Ongoing())
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
          HMSLogger.d(TAG, "join:onPeerUpdate type=$type, peer=$peer")
          when (type) {
            HMSPeerUpdate.PEER_LEFT -> {
              peer.videoTrack?.let { removeTrack(it.trackId) }
            }
            HMSPeerUpdate.BECAME_DOMINANT_SPEAKER -> {
              val videoTrackId = peer.videoTrack?.trackId
              videoTrackId?.let {
                val meetingTrack = getTrackById(videoTrackId)
                meetingTrack?.let {
                  dominantSpeaker.value = it
                }
              }
            }
            else -> Unit
          }
        }

        override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
          HMSLogger.d(TAG, "join:onRoomUpdate type=$type, room=$hmsRoom")
        }

        override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {
          HMSLogger.d(TAG, "join:onTrackUpdate type=$type, track=$track, peer=$peer")
          when (type) {
            HMSTrackUpdate.TRACK_ADDED -> {
              when (track.type) {
                HMSTrackType.AUDIO -> addTrack(
                  MeetingTrack(
                    track.trackId,
                    peerName = peer.name,
                    video = null,
                    audio = (track as HMSRemoteAudioTrack),
                    isCurrentDeviceStream = false,
                    isScreen = false
                  )
                )
                HMSTrackType.VIDEO -> addTrack(
                  MeetingTrack(
                    track.trackId,
                    peerName = peer.name,
                    video = (track as HMSRemoteVideoTrack),
                    audio = null,
                    isCurrentDeviceStream = false,
                    isScreen = false
                  )
                )
              }
            }
            HMSTrackUpdate.TRACK_REMOVED -> {
              if (track.type == HMSTrackType.VIDEO) removeTrack(track.trackId)
            }
            else -> Unit
          }
        }

        override  fun onMessageReceived(message: HMSMessage) {

        }
      })
    }
  }

  fun getTrackById(trackId: String): MeetingTrack? {
    for (track in _videoTracks) {
      if (track.mediaId.equals(trackId, true))
        return track
    }

    return null
  }


  fun flipCamera() {
    if (!settings.publishVideo) {
      error("Cannot switch camera when Video is disabled")
    }

    // NOTE: During audio-only calls, this switch-camera is ignored
    //  as no camera in use
    HMSCoroutineScope.launch {
      localVideoTrack!!.switchCamera()
    }
  }

  fun leaveMeeting() {
    state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
    hmsSDK.leave()
    state.postValue(MeetingState.Disconnected(true))
  }

  private fun addTrack(track: MeetingTrack) {
    synchronized(_videoTracks) {
      if (track.video != null) {
        if (track.isCurrentDeviceStream) {
          _videoTracks.add(0, track)
        } else {
          _videoTracks.add(track)
        }
      } else if (track.audio != null) {
        if (track.isCurrentDeviceStream) {
          _audioTracks.add(0, track)
        } else {
          _audioTracks.add(track)
        }
      }

      videoTracks.postValue(_videoTracks)
      audioTracks.postValue(_audioTracks)
    }
  }

  private fun removeTrack(mid: String) {
    live.hms.video.sdk.NotificationManager
    synchronized(_videoTracks) {
      val trackToRemove = _videoTracks.find { it.mediaId == mid }
      _videoTracks.remove(trackToRemove)

      // Update the view as we have removed some views
      videoTracks.postValue(_videoTracks)
    }
  }

  private fun getLocalScreen() {
    state.postValue(
      MeetingState.LoadingMedia(
        "Loading Media",
        "Getting user local stream"
      )
    )

    // onConnect -> Join -> getUserMedia
  }
}

