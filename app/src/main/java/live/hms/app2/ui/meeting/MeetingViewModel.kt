package live.hms.app2.ui.meeting

import android.app.Application
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
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.utils.HMSCoroutineScope
import live.hms.video.utils.HMSLogger
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

  private val sdk = HMSSDK
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
      _videoTracks.forEach { track ->
        if (track.audio != null && track.audio != localAudioTrack) {
          (track.audio as HMSRemoteAudioTrack).setVolume(volume)
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
        "Establishing websocket connection"
      )
    )

    HMSCoroutineScope.launch {
      val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2Nlc3Nfa2V5IjoiNWY5ZWRjNmJkMjM4MjE1YWVjNzcwMGUyIiwiYXBwX2lkIjoiNWY5ZWRjNmJkMjM4MjE1YWVjNzcwMGUxIiwicm9vbV9pZCI6ImhlbGxvIiwidXNlcl9pZCI6Ijk5ODRhOWRlLWYxOWYtNDFmNi1iZTE4LWQ0MGIzMGFkNGZlZmFkaXR5YSIsInJvbGUiOiJIb3N0IiwiaWF0IjoxNjE3ODk2NjgzLCJleHAiOjE2MTc5ODMwODMsImlzcyI6IjVmOWVkYzZiZDIzODIxNWFlYzc3MDBkZiIsImp0aSI6IjA4NzBhNGQ0LThkZWYtNDgzYS04OGJiLWQ0NjRkMzlhZGYxYiJ9.7WJIaNM6KZqGLqw5ESocQMGIx3b_ckWyu1FNW27gL5E"
      val info = JsonObject().apply { addProperty("name", roomDetails.username) }
      val config = HMSConfig(roomDetails.username, token, info.toString())
      sdk.join(config, object : HMSUpdateListener {
        override fun onError(error: HMSException) {
          Log.e(TAG, error.toString())
          state.postValue(MeetingState.Failure(error))
        }

        override fun onJoin(hmsRoom: HMSRoom) {
          val peer = sdk.getLocalPeer()
          peer.audioTrack?.apply {
            localAudioTrack = (this as HMSLocalAudioTrack)
            addTrack(MeetingTrack(trackId, null, localAudioTrack, true, false))
          }
          peer.videoTrack?.apply {
            localVideoTrack = (this as HMSLocalVideoTrack)
            localVideoTrack!!.startCapturing()
            addTrack(MeetingTrack(trackId, localVideoTrack, null, true, false))
          }
          state.postValue(MeetingState.Ongoing())
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer, track: HMSTrack) {
          when (type) {
            HMSPeerUpdate.TRACK_ADDED -> {
              when (track.type) {
                HMSTrackType.AUDIO -> Unit // TODO
                HMSTrackType.VIDEO -> addTrack(
                  MeetingTrack(
                    track.trackId,
                    (track as HMSRemoteVideoTrack),
                    null,
                    isCurrentDeviceStream = false,
                    isScreen = false
                  )
                )
              }
            }
            HMSPeerUpdate.TRACK_REMOVED -> {
              if (track.type == HMSTrackType.VIDEO) removeTrack(track.trackId)
            }
          }
        }

        override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
          when (type) {
            HMSRoomUpdate.PEER_ADDED -> {
              Log.d(TAG, "onRoomUpdate: $type ")
            }
            HMSRoomUpdate.PEER_REMOVED -> Unit
            else -> Unit
          }
        }

      })
    }
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
    sdk.leave()
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

