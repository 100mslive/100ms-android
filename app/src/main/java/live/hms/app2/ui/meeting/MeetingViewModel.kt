package live.hms.app2.ui.meeting

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.tracks.*
import live.hms.video.signal.grpc.biz.SignalReply
import live.hms.video.transport.HMSTransport
import live.hms.video.transport.ITransportObserver
import live.hms.video.utils.HMSCoroutineScope
import live.hms.video.utils.IdHelper
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

  private val uid = IdHelper.makeStreamId()

  // private val signal = JSONRpcSignal("ws://192.168.29.103:8443/ws?peer=$uid")
  // private val signal = JSONRpcSignal("wss://webrtcv3.100ms.live:8443/ws?peer=$uid")
  private val transport = HMSTransport(application, observer = object : ITransportObserver {
    override fun onFailure(exception: HMSException) {
      Log.e(TAG, exception.toString())
      state.postValue(MeetingState.Failure(exception))

    }

    override fun onNotification(message: JsonObject) {
      broadcastsReceived.postValue(message.toString())
    }

    override fun onTrackAdd(track: HMSTrack) {
      val audio = if (track.type == HMSTrackType.AUDIO) (track as HMSRemoteAudioTrack) else null
      val video = if (track.type == HMSTrackType.VIDEO) (track as HMSRemoteVideoTrack) else null

      addTrack(MeetingTrack(track.trackId, video, audio, true, false))
    }

    override fun onTrackRemove(track: HMSTrack) {
      removeTrack(track.trackId)
    }
  })

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
      val info = JsonObject().apply { addProperty("name", roomDetails.username) }
      transport.join(roomDetails.authToken, roomDetails.roomId, uid, info)
      Log.d(TAG, "Joined")

      val tracks = transport.getLocalTracks(getApplication(), HMSTrackSettings.Builder().build())
      transport.publish(tracks)
      tracks.forEach {
        val audio = if (it.type == HMSTrackType.AUDIO) (it as HMSLocalAudioTrack) else null
        val video = if (it.type == HMSTrackType.VIDEO) (it as HMSLocalVideoTrack) else null
        video?.startCapturing()
        if (audio != null) localAudioTrack = audio
        if (video != null) localVideoTrack = video

        addTrack(MeetingTrack(it.trackId, video, audio, true, false))
      }

      state.postValue(MeetingState.Ongoing())
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
    HMSCoroutineScope.launch {
      transport.leave()
      state.postValue(MeetingState.Disconnected(true))
    }
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

