package live.hms.app2.ui.meeting

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.chat.ChatMessage
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.*
import live.hms.video.sdk.HMSAudioListener
import live.hms.video.sdk.HMSPreviewListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.HMSUpdateListener
import live.hms.video.sdk.models.*
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.sdk.models.role.HMSRole
import live.hms.video.utils.HMSCoroutineScope
import java.util.*
import kotlin.collections.ArrayList

class MeetingViewModel(
  application: Application,
  private val roomDetails: RoomDetails
) : AndroidViewModel(application) {
  companion object {
    private const val TAG = "MeetingViewModel"
  }

  private var pendingRoleChange: HMSRoleChangeRequest? = null
  private val config = HMSConfig(
    roomDetails.username,
    roomDetails.authToken,
    JsonObject().apply { addProperty("name", roomDetails.username) }.toString(),
    initEndpoint = "https://${roomDetails.env}.100ms.live/init" // This is optional paramter, No need to use this in production apps
  )

  init {
    roomDetails.apply {
      crashlytics.setCustomKey(MEETING_URL, url)
      crashlytics.setCustomKey(USERNAME, username)
      crashlytics.setCustomKey(ENVIRONMENT, env)
      crashlytics.setCustomKey(AUTH_TOKEN, authToken)
    }

  }


  private val _tracks = Collections.synchronizedList(ArrayList<MeetingTrack>())

  private val settings = SettingsStore(getApplication())

  private val failures = ArrayList<HMSException>()

  val meetingViewMode = MutableLiveData(settings.meetingMode)

  fun setMeetingViewMode(mode: MeetingViewMode) {
    if (mode != meetingViewMode.value) {
      meetingViewMode.postValue(mode)
    }

  }

  // Title at the top of the meeting
  val title = MutableLiveData<Int>()
  fun setTitle(@StringRes resId: Int) {
    this.title.postValue(resId)
  }

  var showAudioMuted = MutableLiveData(false)
    private set

  // Flag to keep track whether the incoming audio need's to be muted
  private var isAudioMuted: Boolean = false
    set(value) {
      synchronized(_tracks) {
        field = value

        val volume = if (isAudioMuted) 0.0 else 1.0
        _tracks.forEach { track ->
          track.audio?.let {
            if (it is HMSRemoteAudioTrack) {
              it.setVolume(volume)
            }
          }
        }
        showAudioMuted.postValue(value)
      }
    }

  // Live data to define the overall UI
  val state = MutableLiveData<MeetingState>(MeetingState.Disconnected())

  // TODO: Listen to changes in publishVideo & publishAudio
  //  when it is possible to switch from Audio/Video only to Audio+Video/Audio/Video/etc
  // Live data for user media controls
  val isLocalAudioEnabled = MutableLiveData(settings.publishAudio)
  val isLocalVideoEnabled = MutableLiveData(settings.publishVideo)

  private var localAudioTrack: HMSLocalAudioTrack? = null
  private var localVideoTrack: HMSLocalVideoTrack? = null

  // Live data containing all the current tracks in a meeting
  val tracks = MutableLiveData(_tracks)
  val speakers = MutableLiveData<Array<HMSSpeaker>>()

  // Dominant speaker
  val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)

  val broadcastsReceived = MutableLiveData<ChatMessage>()

  private val hmsSDK = HMSSDK
    .Builder(application)
    .build()

  val peers: Array<HMSPeer>
    get() = hmsSDK.getPeers()

  fun <R : Any> mapTracks(transform: (track: MeetingTrack) -> R?): List<R> = synchronized(_tracks) {
    return _tracks.mapNotNull(transform)
  }

  fun getTrackByPeerId(peerId: String): MeetingTrack? = synchronized(_tracks) {
    return _tracks.find { it.peer.peerID == peerId }
  }

  fun findTrack(predicate: (track: MeetingTrack) -> Boolean): MeetingTrack? =
    synchronized(_tracks) {
      return _tracks.find(predicate)
    }

  fun startPreview(listener: HMSPreviewListener) {
    // call Preview api
    hmsSDK.preview(config, listener)
  }

  fun setLocalVideoEnabled(enabled : Boolean) {

    localVideoTrack?.apply {

      setMute(!enabled)

      tracks.postValue(_tracks)

      isLocalVideoEnabled.postValue(enabled)
      crashlyticsLog(TAG, "toggleUserVideo: enabled=$enabled")
    }
  }

  fun isLocalVideoEnabled() : Boolean? = localVideoTrack?.isMute?.not()

  fun toggleLocalVideo() {
      localVideoTrack?.let { setLocalVideoEnabled(it.isMute) }
  }

  fun setLocalAudioEnabled(enabled: Boolean) {

    localAudioTrack?.apply {
      setMute(!enabled)

      tracks.postValue(_tracks)

      isLocalAudioEnabled.postValue(enabled)
      crashlyticsLog(TAG, "toggleUserMic: enabled=$enabled")
    }

  }

  fun isLocalAudioEnabled() : Boolean? {
    return localAudioTrack?.isMute?.not()
  }

  fun toggleLocalAudio() {
    // If mute then enable audio, if not mute, disable it
    localAudioTrack?.let { setLocalAudioEnabled(it.isMute) }
  }

  fun isPeerAudioEnabled() : Boolean = !isAudioMuted

  /**
   * Helper function to toggle others audio tracks
   */
  fun toggleAudio() {
    setPeerAudioEnabled(isAudioMuted)
  }

  fun setPeerAudioEnabled(enabled : Boolean) {
    isAudioMuted = !enabled
  }

  fun sendChatMessage(message: String) {
    hmsSDK.sendMessage("chat", message)
  }

  private fun cleanup() {
    failures.clear()
    _tracks.clear()
    tracks.postValue(_tracks)

    dominantSpeaker.postValue(null)

    localVideoTrack = null
    localAudioTrack = null
  }

  fun startMeeting() {
    if (!(state.value is MeetingState.Disconnected || state.value is MeetingState.Failure)) {
      error("Cannot start meeting in ${state.value} state")
    }

    cleanup()

    state.postValue(
      MeetingState.Connecting(
        "Connecting",
        "Establishing websocket connection"
      )
    )

    HMSCoroutineScope.launch {
      hmsSDK.join(config, object : HMSUpdateListener {
        override fun onError(error: HMSException) {
          Log.e(TAG, "onError: $error")
          failures.add(error)
          state.postValue(MeetingState.Failure(failures))
        }

        override fun onJoin(room: HMSRoom) {
          failures.clear()
/*          val hmsLocalPeer = hmsSDK.getLocalPeer()
          hmsLocalPeer?.audioTrack?.apply {
            localAudioTrack = this
            isLocalAudioEnabled.postValue(!isMute)
            addTrack(this, hmsLocalPeer)
          }
          hmsLocalPeer?.videoTrack?.apply {
            localVideoTrack = this
            isLocalVideoEnabled.postValue(!isMute)
            addTrack(this, hmsLocalPeer)
          }*/

          state.postValue(MeetingState.Ongoing())
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
          Log.d(TAG, "join:onPeerUpdate type=$type, peer=$peer")
          when (type) {
            HMSPeerUpdate.PEER_LEFT -> {
              synchronized(_tracks) {
                _tracks.removeIf { it.peer.peerID == peer.peerID }
                tracks.postValue(_tracks)
              }
            }

            HMSPeerUpdate.BECAME_DOMINANT_SPEAKER -> {
              synchronized(_tracks) {
                val track = _tracks.find {
                  it.peer.peerID == peer.peerID &&
                          it.video?.trackId == peer.videoTrack?.trackId
                }
                if (track != null) dominantSpeaker.postValue(track)
              }
            }

            HMSPeerUpdate.NO_DOMINANT_SPEAKER -> {
              dominantSpeaker.postValue(null)
            }

            else -> Unit
          }
        }

        override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
          Log.d(TAG, "join:onRoomUpdate type=$type, room=$hmsRoom")
        }

        override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {
          Log.d(TAG, "join:onTrackUpdate type=$type, track=$track, peer=$peer")
          when (type) {
            HMSTrackUpdate.TRACK_ADDED -> {
              if (peer is HMSLocalPeer) {
                when (track.type) {
                  HMSTrackType.AUDIO -> {
                    localAudioTrack = track as HMSLocalAudioTrack
                    isLocalAudioEnabled.postValue(!track.isMute)
                  }
                  HMSTrackType.VIDEO -> {
                    localVideoTrack = track as HMSLocalVideoTrack
                    isLocalVideoEnabled.postValue(!track.isMute)
                  }
                }
              }
              addTrack(track, peer)
            }
            HMSTrackUpdate.TRACK_REMOVED -> {
              if (track.type == HMSTrackType.VIDEO) removeTrack(track as HMSVideoTrack, peer)
            }
            HMSTrackUpdate.TRACK_MUTED -> tracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_UNMUTED -> tracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_DESCRIPTION_CHANGED -> tracks.postValue(_tracks)
          }
        }

        override fun onMessageReceived(message: HMSMessage) {
          Log.v(TAG, "onMessageReceived: $message")
          broadcastsReceived.postValue(
            ChatMessage(
              message.sender.name,
              Date(),
              message.message,
              false
            )
          )
        }

        override fun onReconnected() {
          failures.clear()
          state.postValue(MeetingState.Ongoing())
        }

        override fun onReconnecting(error: HMSException) {
          state.postValue(MeetingState.Reconnecting("Reconnecting", error.toString()))
        }

        override fun onRoleChangeRequest(request: HMSRoleChangeRequest) {
          pendingRoleChange = request
          state.postValue(MeetingState.RoleChangeRequest(request))

        }
      })

      hmsSDK.addAudioObserver(object : HMSAudioListener {
        override fun onAudioLevelUpdate(speakers: Array<HMSSpeaker>) {
          Log.v(TAG, "onAudioLevelUpdate: speakers=${speakers.toList()}")
          this@MeetingViewModel.speakers.postValue(speakers)
        }
      })
    }
  }

  fun changeRoleAccept(hmsRoleChangeRequest: HMSRoleChangeRequest) {
    hmsSDK.acceptChangeRole(hmsRoleChangeRequest)
  }


  fun flipCamera() {
    if (!settings.publishVideo) {
      error("Cannot switch camera when Video is disabled")
    }

    // NOTE: During audio-only calls, this switch-camera is ignored
    //  as no camera in use
    HMSCoroutineScope.launch {
      try {
        localVideoTrack?.switchCamera()
      } catch (ex: HMSException) {
        Log.e(TAG, "flipCamera: ${ex.description}", ex)
      }
    }
  }

  fun leaveMeeting() {
    state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
    hmsSDK.leave()
    cleanup()
    state.postValue(MeetingState.Disconnected(true))
  }

  private fun addAudioTrack(track: HMSAudioTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      // Check if this track already exists
      if (track is HMSRemoteAudioTrack) {
        track.setVolume(if (isAudioMuted) 0.0 else 1.0)
      }

      val _track = _tracks.find {
        it.audio == null &&
            it.peer.peerID == peer.peerID &&
            it.isScreen.not()
      }

      if (_track == null) {
        if (peer.isLocal) {
          _tracks.add(0, MeetingTrack(peer, null, track))
        } else {
          _tracks.add(MeetingTrack(peer, null, track))
        }
      } else {
        _track.audio = track
      }

      tracks.postValue(_tracks)
    }
  }


  private fun addVideoTrack(track: HMSVideoTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      // Check if this track already exists
      val _track = _tracks.find { it.video == null && it.peer.peerID == peer.peerID }
      if (_track == null) {
        if (peer.isLocal) {
          _tracks.add(0, MeetingTrack(peer, track, null))
        } else {
          _tracks.add(MeetingTrack(peer, track, null))
        }
      } else {
        _track.video = track
      }
    }

    tracks.postValue(_tracks)
  }

  private fun addTrack(track: HMSTrack, peer: HMSPeer) {
    if (track is HMSAudioTrack) addAudioTrack(track, peer)
    else if (track is HMSVideoTrack) addVideoTrack(track, peer)

    Log.v(TAG, "addTrack: count=${_tracks.size} track=$track, peer=$peer")
  }

  private fun removeTrack(track: HMSVideoTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      val trackToRemove = _tracks.find {
        it.peer.peerID == peer.peerID &&
                it.video?.trackId == track.trackId
      }
      _tracks.remove(trackToRemove)

      // Update the view as we have removed some views
      tracks.postValue(_tracks)
    }
  }

  fun getAvailableRoles(): List<HMSRole> = hmsSDK.getRoles()

  fun changeRole(remotePeer: HMSRemotePeer, toRole: HMSRole) =
    hmsSDK.changeRole(remotePeer, toRole, false)

  fun acceptNewRole(request: HMSRoleChangeRequest) = hmsSDK.acceptChangeRole(request)


}

