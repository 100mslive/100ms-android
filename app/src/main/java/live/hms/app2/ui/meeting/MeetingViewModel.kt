package live.hms.app2.ui.meeting

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.activespeaker.ActiveSpeakerHandler
import live.hms.app2.ui.meeting.chat.ChatMessage
import live.hms.app2.ui.meeting.chat.Recipient
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.*
import live.hms.video.sdk.*
import live.hms.video.sdk.models.*
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.sdk.models.role.HMSRole
import live.hms.video.sdk.models.trackchangerequest.HMSChangeTrackStateRequest
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

  // Live data for enabling/disabling mute buttons
  val isLocalAudioPublishingAllowed = MutableLiveData(false)
  val isLocalVideoPublishingAllowed = MutableLiveData(false)

  private var localAudioTrack: HMSLocalAudioTrack? = null
  private var localVideoTrack: HMSLocalVideoTrack? = null

  // Live data containing all the current tracks in a meeting
  val tracks = MutableLiveData(_tracks)

  // Live data containing the current Speaker in the meeting
  val speakers = MutableLiveData<Array<HMSSpeaker>>()

  private val activeSpeakerHandler = ActiveSpeakerHandler { _tracks }
  val activeSpeakers: LiveData<Pair<List<MeetingTrack>, Array<HMSSpeaker>>> =
    speakers.map(activeSpeakerHandler::speakerUpdate)
  val activeSpeakersUpdatedTracks = tracks.map(activeSpeakerHandler::trackUpdateLruTrigger)

  // Live data which changes on any change of peer
  val peerLiveDate = MutableLiveData<HMSPeer>()

  // Dominant speaker
  val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)

  val broadcastsReceived = MutableLiveData<ChatMessage>()

  val hmsSDK = HMSSDK
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

  fun setLocalVideoEnabled(enabled: Boolean) {

    localVideoTrack?.apply {

      setMute(!enabled)

      tracks.postValue(_tracks)

      isLocalVideoEnabled.postValue(enabled)
      crashlyticsLog(TAG, "toggleUserVideo: enabled=$enabled")
    }
  }

  fun isLocalVideoEnabled(): Boolean? = localVideoTrack?.isMute?.not()

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

  fun isLocalAudioEnabled(): Boolean? {
    return localAudioTrack?.isMute?.not()
  }

  fun toggleLocalAudio() {
    // If mute then enable audio, if not mute, disable it
    localAudioTrack?.let { setLocalAudioEnabled(it.isMute) }
  }

  fun isPeerAudioEnabled(): Boolean = !isAudioMuted

  /**
   * Helper function to toggle others audio tracks
   */
  fun toggleAudio() {
    setPeerAudioEnabled(isAudioMuted)
  }

  fun setPeerAudioEnabled(enabled: Boolean) {
    isAudioMuted = !enabled
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
          state.postValue(MeetingState.Ongoing())
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, hmsPeer: HMSPeer) {
          Log.d(TAG, "join:onPeerUpdate type=$type, peer=$hmsPeer")
          when (type) {
            HMSPeerUpdate.PEER_LEFT -> {
              synchronized(_tracks) {
                _tracks.removeIf { it.peer.peerID == hmsPeer.peerID }
                tracks.postValue(_tracks)
                peerLiveDate.postValue(hmsPeer)
              }
            }

            HMSPeerUpdate.PEER_JOINED -> {
              peerLiveDate.postValue(hmsPeer)
            }

            HMSPeerUpdate.BECAME_DOMINANT_SPEAKER -> {
              synchronized(_tracks) {
                val track = _tracks.find {
                  it.peer.peerID == hmsPeer.peerID &&
                          it.video?.trackId == hmsPeer.videoTrack?.trackId
                }
                if (track != null) dominantSpeaker.postValue(track)
              }
            }

            HMSPeerUpdate.NO_DOMINANT_SPEAKER -> {
              dominantSpeaker.postValue(null)
            }

            HMSPeerUpdate.ROLE_CHANGED -> {
              peerLiveDate.postValue(hmsPeer)
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
                    isLocalAudioPublishingAllowed.postValue(true)
                    isLocalAudioEnabled.postValue(!track.isMute)
                  }
                  HMSTrackType.VIDEO -> {
                    localVideoTrack = track as HMSLocalVideoTrack
                    isLocalVideoPublishingAllowed.postValue(true)
                    isLocalVideoEnabled.postValue(!track.isMute)
                  }
                }
              }
              addTrack(track, peer)
            }
            HMSTrackUpdate.TRACK_REMOVED -> {
              if (peer is HMSLocalPeer) {
                when (track.type) {
                  HMSTrackType.AUDIO -> {
                    isLocalAudioPublishingAllowed.postValue(false)
                  }
                  HMSTrackType.VIDEO -> {
                    isLocalVideoPublishingAllowed.postValue(false)
                  }
                }
              }
              removeTrack(track, peer)
            }
            HMSTrackUpdate.TRACK_MUTED -> {
              tracks.postValue(_tracks)
              if (peer.isLocal) {
                if(track.type == HMSTrackType.AUDIO)
                  isLocalAudioEnabled.postValue(peer.audioTrack?.isMute != true)
                else if(track.type == HMSTrackType.VIDEO){
                  isLocalVideoEnabled.postValue(peer.videoTrack?.isMute != true)
                }
              }
            }
            HMSTrackUpdate.TRACK_UNMUTED -> tracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_DESCRIPTION_CHANGED -> tracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_DEGRADED -> tracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_RESTORED -> tracks.postValue(_tracks)
          }
        }

        override fun onMessageReceived(message: HMSMessage) {
          Log.v(TAG, "onMessageReceived: $message")
          broadcastsReceived.postValue(
            ChatMessage(
              message.sender.name,
              message.serverReceiveTime,
              message.message,
              false,
              recipient = Recipient.toRecipient(message.recipient)
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

        override fun onRemovedFromRoom(notification: HMSRemovedFromRoom) {
          // Display a dialog that says they've been removed by X for Y with an ok button.
          state.postValue(MeetingState.ForceLeave(notification))
        }

        override fun onRoleChangeRequest(request: HMSRoleChangeRequest) {
          pendingRoleChange = request
          state.postValue(MeetingState.RoleChangeRequest(request))
        }

        override fun onChangeTrackStateRequest(details: HMSChangeTrackStateRequest) {
          state.postValue(MeetingState.TrackChangeRequest(details))
        }
      })

      hmsSDK.addAudioObserver(object : HMSAudioListener {
        override fun onAudioLevelUpdate(speakers: Array<HMSSpeaker>) {
          Log.d(
            TAG,
            speakers.fold("Customer_User_IDs:") { cur: String, sp: HMSSpeaker -> cur + " ${sp.peer?.customerUserID}" })
          Log.v(TAG, "onAudioLevelUpdate: speakers=${speakers.toList()}")
          this@MeetingViewModel.speakers.postValue(speakers)
        }
      })
    }
  }

  fun setStatetoOngoing() {
    state.postValue(MeetingState.Ongoing())
  }

  fun changeRoleAccept(hmsRoleChangeRequest: HMSRoleChangeRequest) {
    hmsSDK.acceptChangeRole(hmsRoleChangeRequest, object : HMSActionResultListener{
      override fun onSuccess() {
        Log.i(TAG, "Successfully accepted change role request for $hmsRoleChangeRequest")
      }

      override fun onError(error: HMSException) {
        Log.e(TAG, "Error while accepting change role request :: ${error.description}")
        state.postValue(MeetingState.NonFatalFailure(error))
      }
    })
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

  fun leaveMeeting(details: HMSRemovedFromRoom? = null) {
    state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
    hmsSDK.leave()
    cleanup()
    state.postValue(MeetingState.Disconnected(true, details))
  }

  private fun addAudioTrack(track: HMSAudioTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      if (track is HMSRemoteAudioTrack) {
        track.setVolume(if (isAudioMuted) 0.0 else 1.0)
      }

      // Check if this track is of screenshare type, then we dont need to show a tile
      if (track.source == HMSTrackSource.SCREEN)
        return

      // Check if this track already exists
      val _track = _tracks.find {
                it.peer.peerID == peer.peerID &&
                it.isScreen.not()
      }

      if (_track == null) {
        // No existing MeetingTrack found, add a new tile
        if (peer.isLocal) {
          _tracks.add(0, MeetingTrack(peer, null, track))
        } else {
          _tracks.add(MeetingTrack(peer, null, track))
        }
      } else {
        // Existing MeetingTrack, update its audio track
        _track.audio = track
      }

      tracks.postValue(_tracks)
    }
  }

  private fun addVideoTrack(track: HMSVideoTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      if (track.source == HMSTrackSource.SCREEN) {
        // Add a new tile to show screen share
        _tracks.add(MeetingTrack(peer, track, null))
      } else {
        // First check if this track already exists
        val _track = _tracks.find { it.peer.peerID == peer.peerID && it.isScreen.not() }
        if (_track == null) {
          // No existing MeetingTrack found, add a new tile
          if (peer.isLocal) {
            _tracks.add(0, MeetingTrack(peer, track, null))
          } else {
            _tracks.add(MeetingTrack(peer, track, null))
          }
        } else {
          // Existing MeetingTrack, update its video track
          _track.video = track
        }
      }
    }

    tracks.postValue(_tracks)
  }

  private fun addTrack(track: HMSTrack, peer: HMSPeer) {
    if (track is HMSAudioTrack) addAudioTrack(track, peer)
    else if (track is HMSVideoTrack) addVideoTrack(track, peer)

    Log.v(TAG, "addTrack: count=${_tracks.size} track=$track, peer=$peer")
  }

  private fun removeTrack(track: HMSTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      val meetingTrack = when (track.type) {
        HMSTrackType.AUDIO -> {
          _tracks.find {
            it.peer.peerID == peer.peerID &&
                    it.audio?.trackId == track.trackId
          }
        }
        HMSTrackType.VIDEO -> {
          _tracks.find {
            it.peer.peerID == peer.peerID &&
                    it.video?.trackId == track.trackId
          }
        }
      }

      if (
        // Remove tile from view since both audio and video track are null for the peer
        (peer.audioTrack == null &&  peer.videoTrack == null) ||
        // Remove video screenshare tile from view
        (track.source == HMSTrackSource.SCREEN && track.type == HMSTrackType.VIDEO)) {
        _tracks.remove(meetingTrack)
      }

      // Update the view as some track has been removed
      tracks.postValue(_tracks)
    }
  }

  fun getAvailableRoles(): List<HMSRole> = hmsSDK.getRoles()

  fun isAllowedToChangeRole(): Boolean {
    return hmsSDK.getLocalPeer()?.hmsRole?.permission?.changeRole == true
  }

  fun isAllowedToEndMeeting(): Boolean {
    return hmsSDK.getLocalPeer()?.hmsRole?.permission?.endRoom == true
  }

  fun isAllowedToRemovePeers(): Boolean {
    return hmsSDK.getLocalPeer()?.hmsRole?.permission?.removeOthers == true
  }

  fun isAllowedToMutePeers(): Boolean {
    return hmsSDK.getLocalPeer()?.hmsRole?.permission?.mute == true
  }

  fun isAllowedToAskUnmutePeers(): Boolean {
    return hmsSDK.getLocalPeer()?.hmsRole?.permission?.unmute == true
  }

  fun changeRole(remotePeerId: String, toRoleName: String, force: Boolean) {
    val hmsPeer = hmsSDK.getPeers().find { it.peerID == remotePeerId }
    val toRole = hmsSDK.getRoles().find { it.name == toRoleName }
    if (hmsPeer != null && toRole != null) {
      if (hmsPeer.hmsRole.name != toRole.name)
        hmsSDK.changeRole(hmsPeer, toRole, force,object : HMSActionResultListener{
          override fun onSuccess() {
            Log.i(TAG, "Successfully sent change role request for $hmsPeer")
          }

          override fun onError(error: HMSException) {
            Log.e(TAG, "Error while sending change role request :: ${error.description}")
            state.postValue(MeetingState.NonFatalFailure(error))
          }
        })
      // Update the peer in participants
      peerLiveDate.postValue(hmsPeer)
    }
  }

  fun requestPeerLeave(hmsPeer: HMSRemotePeer, reason: String) {
    hmsSDK.removePeerRequest(hmsPeer, reason, object : HMSActionResultListener{
      override fun onError(error: HMSException) {
        state.postValue(MeetingState.NonFatalFailure(error))
      }

      override fun onSuccess() {
        // Request Successfully sent to server
      }
    })
  }

  fun endRoom(lock: Boolean) {
    hmsSDK.endRoom("Closing time", lock, object : HMSActionResultListener{
      override fun onError(error: HMSException) {
        state.postValue(MeetingState.NonFatalFailure(error))
      }

      override fun onSuccess() {
        // Request Successfully sent to server
      }
    })
    leaveMeeting()
  }

  fun togglePeerMute(hmsPeer: HMSRemotePeer, type: HMSTrackType) {

    val track = when(type) {
      HMSTrackType.AUDIO -> hmsPeer.audioTrack
      HMSTrackType.VIDEO -> hmsPeer.videoTrack
    }

    if (track != null) {
      val isMute = track.isMute
      if (isAllowedToAskUnmutePeers() && isMute) {
        hmsSDK.changeTrackState(track, false, object : HMSActionResultListener{
          override fun onError(error: HMSException) {
            state.postValue(MeetingState.NonFatalFailure(error))
          }

          override fun onSuccess() {
            // Request Successfully sent to server
          }

        })
      } else if (isAllowedToMutePeers() && !isMute) {
        hmsSDK.changeTrackState(track, true, object : HMSActionResultListener{
          override fun onError(error: HMSException) {
            state.postValue(MeetingState.NonFatalFailure(error))
          }

          override fun onSuccess() {
            // Request Successfully sent to server
          }
        })
      }
    } else {
      Log.d(TAG, "track was null")
    }
  }

  fun getPeerForId(peerId : String) : HMSPeer? {
    return hmsSDK.getPeers().find { it.peerID == peerId }
  }

}

