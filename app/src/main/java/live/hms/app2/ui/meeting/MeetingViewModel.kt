package live.hms.app2.ui.meeting
import android.R
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.activespeaker.ActiveSpeakerHandler
import live.hms.app2.ui.meeting.chat.ChatMessage
import live.hms.app2.ui.meeting.chat.Recipient
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import live.hms.video.connection.stats.*
import live.hms.video.error.HMSException
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSLogSettings
import live.hms.video.media.settings.HMSRtmpVideoResolution
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.tracks.*
import live.hms.video.sdk.*
import live.hms.video.sdk.models.*
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.sdk.models.role.HMSRole
import live.hms.video.sdk.models.trackchangerequest.HMSChangeTrackStateRequest
import live.hms.video.services.HMSScreenCaptureService
import live.hms.video.services.LogAlarmManager
import live.hms.video.utils.HMSCoroutineScope
import live.hms.video.utils.HMSLogger
import live.hms.video.virtualbackground.HMSVirtualBackground
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random


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
    Gson().toJson(CustomPeerMetadata(isHandRaised = false, name = roomDetails.username)).toString(),
    captureNetworkQualityInPreview = true,
    initEndpoint = "https://${roomDetails.env}.100ms.live/init" // This is optional paramter, No need to use this in production apps
  )

  private val recordingTimesUseCase = RecordingTimesUseCase()

  private fun showServerInfo(room : HMSRoom) {
    viewModelScope.launch {
      _events.emit(Event.ServerRecordEvent(recordingTimesUseCase.showServerInfo(room)))
    }
  }

  private fun showRecordInfo(room : HMSRoom) {
    viewModelScope.launch {
      _events.emit(Event.RecordEvent(recordingTimesUseCase.showRecordInfo(room)))
    }
  }

  private fun showRtmpInfo(room : HMSRoom) {
    viewModelScope.launch {
      _events.emit(Event.RtmpEvent(recordingTimesUseCase.showRtmpInfo(room)))
    }
  }

  private fun showHlsInfo(room : HMSRoom) {
    viewModelScope.launch {
      _events.emit(Event.HlsEvent(recordingTimesUseCase.showHlsInfo(room, false)))
    }
  }

  private fun showHlsRecordingInfo(room : HMSRoom) {
    viewModelScope.launch {
      _events.emit(Event.HlsRecordingEvent(recordingTimesUseCase.showHlsInfo(room, true)))
    }
  }

  init {
    roomDetails.apply {
      crashlytics.setCustomKey(MEETING_URL, url)
      crashlytics.setCustomKey(USERNAME, username)
      crashlytics.setCustomKey(ENVIRONMENT, env)
      crashlytics.setCustomKey(AUTH_TOKEN, authToken)
    }

  }

  private val _tracks = Collections.synchronizedList(ArrayList<MeetingTrack>())

  // When we get stats, a flow will be updated with the saved stats.
  private val statsFlow = MutableSharedFlow<Map<String,HMSStats>>()
  private val savedStats : MutableMap<String, HMSStats> = mutableMapOf()

  private val settings = SettingsStore(getApplication())

  private val failures = ArrayList<HMSException>()

  val meetingViewMode = MutableLiveData(settings.meetingMode)

  private val roomState : MutableLiveData<Pair<HMSRoomUpdate, HMSRoom>> = MutableLiveData()
  private val previewPeerData : MutableLiveData<Pair<HMSPeerUpdate, HMSPeer>> = MutableLiveData()
  private val previewErrorData : MutableLiveData<HMSException> = MutableLiveData()
  private val previewUpdateData : MutableLiveData<Pair<HMSRoom, Array<HMSTrack>>> = MutableLiveData()

  val previewRoomStateLiveData : LiveData<Pair<HMSRoomUpdate, HMSRoom>> = roomState
  val previewPeerLiveData : LiveData<Pair<HMSPeerUpdate, HMSPeer>> = previewPeerData
  val previewErrorLiveData : LiveData<HMSException> = previewErrorData
  val previewUpdateLiveData : LiveData<Pair<HMSRoom, Array<HMSTrack>>> = previewUpdateData


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

        // Setting of volume greater than 1 is causing to increase the gain, rather
        // than volume
        // 1.0 is the default value of webrtc
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

  private val _isRecording = MutableLiveData(RecordingState.NOT_RECORDING_OR_STREAMING)
  val isRecording: LiveData<RecordingState> = _isRecording
  private var hmsRoom: HMSRoom? = null

  // Live data for enabling/disabling mute buttons
  val isLocalAudioPublishingAllowed = MutableLiveData(false)
  val isLocalVideoPublishingAllowed = MutableLiveData(false)

  // Live data containing all the current tracks in a meeting
  private val _liveDataTracks = MutableLiveData(_tracks)
  val tracks: LiveData<List<MeetingTrack>> = _liveDataTracks

  // Live data containing the current Speaker in the meeting
  val speakers = MutableLiveData<Array<HMSSpeaker>>()

  private val activeSpeakerHandler = ActiveSpeakerHandler { _tracks }
  val activeSpeakers: LiveData<Pair<List<MeetingTrack>, Array<HMSSpeaker>>> =
    speakers.map(activeSpeakerHandler::speakerUpdate)
  val activeSpeakersUpdatedTracks = _liveDataTracks.map(activeSpeakerHandler::trackUpdateTrigger)

  // Live data which changes on any change of peer
  val peerLiveDate = MutableLiveData<HMSPeer>()
  private val _peerMetadataNameUpdate = MutableLiveData<Pair<HMSPeer, HMSPeerUpdate>>()
  val peerMetadataNameUpdate: LiveData<Pair<HMSPeer, HMSPeerUpdate>> = _peerMetadataNameUpdate

  // Dominant speaker
  val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)

  val broadcastsReceived = MutableLiveData<ChatMessage>()

  private val _trackStatus = MutableLiveData<String>()
  val trackStatus: LiveData<String> = _trackStatus

  private val hmsTrackSettings = HMSTrackSettings.Builder()
    .audio(
      HMSAudioTrackSettings.Builder()
        .setUseHardwareAcousticEchoCanceler(settings.enableHardwareAEC).build()
    )
    .build()

  private val hmsLogSettings : HMSLogSettings = HMSLogSettings(LogAlarmManager.DEFAULT_DIR_SIZE,true)

  val hmsSDK = HMSSDK
    .Builder(application)
    .setTrackSettings(hmsTrackSettings) // SDK uses HW echo cancellation, if nothing is set in builder
    .setLogSettings(hmsLogSettings)
    .build()

  val imageBitmap = getRandomVirtualBackgroundBitmap(application.applicationContext)
  private val virtualBackgroundPlugin = HMSVirtualBackground(hmsSDK, imageBitmap)

  val peers: List<HMSPeer>
    get() = hmsSDK.getPeers().toList()

  fun startPreview() {
    // call Preview api
    hmsSDK.preview(config, object : HMSPreviewListener{
      override fun onError(error: HMSException) {
        previewErrorData.postValue(error)
      }

      override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
        previewPeerData.postValue(Pair(type,peer))
      }

      override fun onPreview(room: HMSRoom, localTracks: Array<HMSTrack>) {
        previewUpdateData.postValue(Pair(room,localTracks))
      }

      override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
        roomState.postValue(Pair(type,hmsRoom))
        // This will keep the isRecording value updated correctly in preview. It will not be called after join.
        _isRecording.postValue(getRecordingState(hmsRoom))
      }

    })
  }

  fun setLocalVideoEnabled(enabled: Boolean) {

    hmsSDK.getLocalPeer()?.videoTrack?.apply {

      setMute(!enabled)

      _liveDataTracks.postValue(_tracks)

      isLocalVideoEnabled.postValue(enabled)
      crashlyticsLog(TAG, "toggleUserVideo: enabled=$enabled")
    }
  }

  fun isLocalVideoEnabled(): Boolean? = hmsSDK.getLocalPeer()?.videoTrack?.isMute?.not()

  fun toggleLocalVideo() {
    hmsSDK.getLocalPeer()?.videoTrack?.let {
      setLocalVideoEnabled(it.isMute)
    }
  }

  fun setLocalAudioEnabled(enabled: Boolean) {

    hmsSDK.getLocalPeer()?.audioTrack?.apply {
      setMute(!enabled)

      _liveDataTracks.postValue(_tracks)

      isLocalAudioEnabled.postValue(enabled)
      crashlyticsLog(TAG, "toggleUserMic: enabled=$enabled")
    }

  }

  fun isLocalAudioEnabled(): Boolean? {
    return hmsSDK.getLocalPeer()?.audioTrack?.isMute?.not()
  }

  fun toggleLocalAudio() {
    // If mute then enable audio, if not mute, disable it
    hmsSDK.getLocalPeer()?.audioTrack?.let { setLocalAudioEnabled(it.isMute) }
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
    _liveDataTracks.postValue(_tracks)

    dominantSpeaker.postValue(null)
  }

  fun addRTCStatsObserver() {
      hmsSDK.addRtcStatsObserver(object : HMSStatsObserver {
      override fun onLocalAudioStats(
        audioStats: HMSLocalAudioStats,
        hmsTrack: HMSTrack?,
        hmsPeer: HMSPeer?
      ) {
        Log.d("RtcStatsObserver","Local VideoStats: $audioStats")
        val id = hmsTrack?.trackId
        if(id != null)
          savedStats[id] = audioStats
      }

      override fun onLocalVideoStats(
        videoStats: HMSLocalVideoStats,
        hmsTrack: HMSTrack?,
        hmsPeer: HMSPeer?
      ) {
        Log.d("RtcStatsObserver","Local VideoStats: $videoStats")
        val id = hmsTrack?.trackId
        if(id != null)
          savedStats[id] = videoStats

      }

      override fun onRTCStats(rtcStats: HMSRTCStatsReport) {
        Log.d("RtcStatsObserver","Cumulative stats: $rtcStats")
        viewModelScope.launch {
          statsFlow.emit(savedStats)
        }
      }

      override fun onRemoteAudioStats(
        audioStats: HMSRemoteAudioStats,
        hmsTrack: HMSTrack?,
        hmsPeer: HMSPeer?
      ) {
        Log.d("RtcStatsObserver","Remote audio stats: $audioStats for peer : ${hmsPeer?.name}, with track : ${hmsTrack?.trackId}")
        val id = hmsTrack?.trackId
        if(id != null)
          savedStats[id] = audioStats
      }

      override fun onRemoteVideoStats(
        videoStats: HMSRemoteVideoStats,
        hmsTrack: HMSTrack?,
        hmsPeer: HMSPeer?
      ) {
        Log.d("RtcStatsObserver","Remote video stats: $videoStats for peer : ${hmsPeer?.name}, with track : ${hmsTrack?.trackId}")
        val id = hmsTrack?.trackId
        if(id != null)
          savedStats[id] = videoStats
      }

    })
  }

  fun removeRtcStatsObserver() {
    hmsSDK.removeRtcStatsObserver()
  }

  fun startMeeting() {

    if(settings.showStats) {
      addRTCStatsObserver()
    }

    if (!(state.value is MeetingState.Disconnected || state.value is MeetingState.Failure || state.value is MeetingState.NonFatalFailure)) {
      error("Cannot start meeting in ${state.value} state")
    }

    cleanup()

    state.postValue(
      MeetingState.Connecting(
        "Connecting",
        "Establishing websocket connection"
      )
    )


      Log.v(TAG, "~~ hmsSDK.join called ~~")
      hmsSDK.join(config, object : HMSUpdateListener {

        override fun onError(error: HMSException) {
          Log.e(TAG, "onError: $error")
          // Show a different dialog if error is terminal else a dismissible dialog
          if (error.isTerminal) {
            failures.add(error)
            state.postValue(MeetingState.Failure(failures))
          } else {
            state.postValue(MeetingState.NonFatalFailure(error))
          }
        }

        override fun onJoin(room: HMSRoom) {
          Log.v(TAG, "~~ onJoin called ~~")
          failures.clear()
          state.postValue(MeetingState.Ongoing())
          hmsRoom = room // Just storing the room id for the beam bot.
          Log.d("onRoomUpdate", "$room")
          Log.d(TAG, "SessionId is: ${room.sessionId}")

          // get the hls URL from the Room, if it exists
          val hlsUrl = room.hlsStreamingState?.variants?.get(0)?.hlsStreamUrl
          switchToHlsViewIfRequired(room.localPeer?.hmsRole, hlsUrl)
          _isRecording.postValue(
            getRecordingState(room)
          )
        }

        override fun onPeerUpdate(type: HMSPeerUpdate, hmsPeer: HMSPeer) {
          Log.d(TAG, "join:onPeerUpdate type=$type, peer=$hmsPeer")
          when (type) {
            HMSPeerUpdate.PEER_LEFT -> {
              synchronized(_tracks) {
                _tracks.removeIf { it.peer.peerID == hmsPeer.peerID }
                _liveDataTracks.postValue(_tracks)
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
              if (hmsPeer.isLocal) {
                // get the hls URL from the Room, if it exists
                val hlsUrl = hmsRoom?.hlsStreamingState?.variants?.get(0)?.hlsStreamUrl
                val isHlsPeer = isHlsPeer(hmsPeer.hmsRole)
                if(isHlsPeer) {
                  switchToHlsViewIfRequired(hmsPeer.hmsRole, hlsUrl)
                } else {
                  exitHlsViewIfRequired(false)
                }
              }
            }

            HMSPeerUpdate.METADATA_CHANGED -> {
              if (hmsPeer.isLocal) {
                updateSelfHandRaised(hmsPeer as HMSLocalPeer)
              } else {
                _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
              }
            }
            HMSPeerUpdate.NAME_CHANGED -> {
              if (hmsPeer.isLocal) {
                updateNameChange(hmsPeer as HMSLocalPeer)
              } else {
                _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
              }
            }

            HMSPeerUpdate.NETWORK_QUALITY_UPDATED -> {
              _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
            }

            else -> Unit
          }
        }

        override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
          Log.d(TAG, "join:onRoomUpdate type=$type, room=$hmsRoom")

          when (type) {
            HMSRoomUpdate.SERVER_RECORDING_STATE_UPDATED -> {
              _isRecording.postValue(
                getRecordingState(hmsRoom)
              )
              showServerInfo(hmsRoom)
            }
            HMSRoomUpdate.RTMP_STREAMING_STATE_UPDATED -> {
              _isRecording.postValue(
                getRecordingState(hmsRoom)
              )
              showRtmpInfo(hmsRoom)
            }
            HMSRoomUpdate.BROWSER_RECORDING_STATE_UPDATED -> {
              _isRecording.postValue(
                getRecordingState(hmsRoom)
              )
              showRecordInfo(hmsRoom)
            }
            HMSRoomUpdate.HLS_STREAMING_STATE_UPDATED -> {
              _isRecording.postValue(
                getRecordingState(hmsRoom)
              )
              switchToHlsViewIfRequired()
              showHlsInfo(hmsRoom)
            }
            HMSRoomUpdate.HLS_RECORDING_STATE_UPDATED -> {
              _isRecording.postValue(
                getRecordingState(hmsRoom)
              )
              showHlsRecordingInfo(hmsRoom)
            }
            else -> {
            }
          }
        }

        override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {
          Log.d(TAG, "join:onTrackUpdate type=$type, track=$track, peer=$peer")
          when (type) {
            HMSTrackUpdate.TRACK_ADDED -> {
              if (peer is HMSLocalPeer && track.source == HMSTrackSource.REGULAR) {
                when (track.type) {
                  HMSTrackType.AUDIO -> {
                    isLocalAudioPublishingAllowed.postValue(true)
                    isLocalAudioEnabled.postValue(!track.isMute)
                  }
                  HMSTrackType.VIDEO -> {
                    isLocalVideoPublishingAllowed.postValue(true)
                    isLocalVideoEnabled.postValue(!track.isMute)
                  }
                }
              }
              addTrack(track, peer)
            }
            HMSTrackUpdate.TRACK_REMOVED -> {
              if (peer is HMSLocalPeer && track.source == HMSTrackSource.REGULAR) {
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
              _liveDataTracks.postValue(_tracks)
              if (peer.isLocal) {
                if (track.type == HMSTrackType.AUDIO)
                  isLocalAudioEnabled.postValue(peer.audioTrack?.isMute != true)
                else if (track.type == HMSTrackType.VIDEO) {
                  isLocalVideoEnabled.postValue(peer.videoTrack?.isMute != true)
                }
              }
            }
            HMSTrackUpdate.TRACK_UNMUTED -> {
              _liveDataTracks.postValue(_tracks)
              if (peer.isLocal) {
                if (track.type == HMSTrackType.AUDIO)
                  isLocalAudioEnabled.postValue(peer.audioTrack?.isMute != true)
                else if (track.type == HMSTrackType.VIDEO) {
                  isLocalVideoEnabled.postValue(peer.videoTrack?.isMute != true)
                }
              }
            }
            HMSTrackUpdate.TRACK_DESCRIPTION_CHANGED -> _liveDataTracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_DEGRADED -> _liveDataTracks.postValue(_tracks)
            HMSTrackUpdate.TRACK_RESTORED -> _liveDataTracks.postValue(_tracks)
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
          HMSLogger.d(TAG, "~~ onReconnected ~~")
          failures.clear()
          state.postValue(MeetingState.Reconnected())
        }

        override fun onReconnecting(error: HMSException) {
          HMSLogger.d(TAG, "~~ onReconnecting :: $error ~~")
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
          viewModelScope.launch {
            if (details.track.isMute != details.mute) {
              _events.emit(Event.ChangeTrackMuteRequest(details))
            }
          }
        }
      })

      hmsSDK.addAudioObserver(object : HMSAudioListener {
        override fun onAudioLevelUpdate(speakers: Array<HMSSpeaker>) {
          HMSLogger.v(
            TAG,
            "onAudioLevelUpdate: speakers=${speakers.map { Pair(it.peer?.name, it.level) }}"
          )
          this@MeetingViewModel.speakers.postValue(speakers)
        }
      })
  }

  private fun updateSelfHandRaised(hmsPeer: HMSLocalPeer) {
    val isSelfHandRaised = CustomPeerMetadata.fromJson(hmsPeer.metadata)?.isHandRaised == true
    _isHandRaised.postValue(isSelfHandRaised)
    _peerMetadataNameUpdate.postValue(Pair(hmsPeer, HMSPeerUpdate.METADATA_CHANGED))
  }

  private fun updateNameChange(hmsPeer: HMSLocalPeer) {
    _peerMetadataNameUpdate.postValue(Pair(hmsPeer, HMSPeerUpdate.NAME_CHANGED))
  }

  private fun getRecordingState(room: HMSRoom): RecordingState {

    val recording = room.browserRecordingState?.running == true ||
            room.serverRecordingState?.running == true ||
            room.hlsRecordingState?.running == true
    val streaming = room.rtmpHMSRtmpStreamingState?.running == true ||
            room.hlsStreamingState?.running == true

    return if (recording && streaming) {
      RecordingState.STREAMING_AND_RECORDING
    } else if (recording) {
      RecordingState.RECORDING
    } else if (streaming) {
      RecordingState.STREAMING
    } else {
      RecordingState.NOT_RECORDING_OR_STREAMING
    }
  }

  fun setStatetoOngoing() {
    state.postValue(MeetingState.Ongoing())
  }

  fun changeRoleAccept(hmsRoleChangeRequest: HMSRoleChangeRequest) {
    hmsSDK.acceptChangeRole(hmsRoleChangeRequest, object : HMSActionResultListener {
      override fun onSuccess() {
        Log.i(TAG, "Successfully accepted change role request for $hmsRoleChangeRequest")
      }

      override fun onError(error: HMSException) {
        Log.e(TAG, "Error while accepting change role request :: ${error.description}")
        state.postValue(MeetingState.NonFatalFailure(error))
      }
    })
  }

  private fun isHlsPeer(role : HMSRole?) : Boolean =
    role?.name?.startsWith("hls-") == true

  private fun switchToHlsView(streamUrl : String) =
    meetingViewMode.postValue(MeetingViewMode.HLS(streamUrl))

  private fun exitHlsViewIfRequired(isHlsPeer: Boolean) {
    if(!isHlsPeer && meetingViewMode.value is MeetingViewMode.HLS) {
      meetingViewMode.postValue(MeetingViewMode.ACTIVE_SPEAKER)
    }
  }

  private fun switchToHlsViewIfRequired(role : HMSRole?, streamUrl: String?) {
    var started = false
    val isHlsPeer = isHlsPeer(role)
    if( isHlsPeer && streamUrl != null) {
      started = true
      switchToHlsView(streamUrl)
    }

    // Only send errors for those who are hls peers
    if(!started && isHlsPeer) {
      val reasons = mutableListOf<String>()
      if(streamUrl == null) {
        reasons.add("Stream url was null")
      }
      HMSCoroutineScope.launch {
        _events.emit(Event.HlsNotStarted("Can't switch to hls view. ${reasons.joinToString(",")}"))
      }
    }
  }

  fun switchToHlsViewIfRequired() {
    // get the hls URL from the Room, if it exists
    val hlsUrl = hmsSDK.getRoom()?.hlsStreamingState?.variants?.get(0)?.hlsStreamUrl
    switchToHlsViewIfRequired(hmsSDK.getLocalPeer()?.hmsRole, hlsUrl)
  }

  fun flipCamera() {
    if (!settings.publishVideo) {
      error("Cannot switch camera when Video is disabled")
    }

    // NOTE: During audio-only calls, this switch-camera is ignored
    //  as no camera in use
    try {
      HMSCoroutineScope.launch(Dispatchers.Main) {
        hmsSDK.getLocalPeer()?.videoTrack?.switchCamera()
      }
    } catch (ex: HMSException) {
      Log.e(TAG, "flipCamera: ${ex.description}", ex)
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
      if (isAudioMuted && track is HMSRemoteAudioTrack) {
        track.setVolume(0.0) // Only keep people muted. Don't unmute those who come in add track because it might break SDK level muting.
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

      _liveDataTracks.postValue(_tracks)
    }
  }

  private fun addVideoTrack(track: HMSVideoTrack, peer: HMSPeer) {
    synchronized(_tracks) {
      if (track.source == HMSTrackSource.SCREEN ||
              track.source == "videoplaylist") {
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

    _liveDataTracks.postValue(_tracks)
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
        // Remove video screenshare/playlist tile from view
        ((track.source == HMSTrackSource.SCREEN || track.source == "videoplaylist")
                && track.type == HMSTrackType.VIDEO)) {
        _tracks.remove(meetingTrack)
      }

      // Update the view as some track has been removed
      _liveDataTracks.postValue(_tracks)
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
        hmsSDK.changeRole(hmsPeer, toRole, force, object : HMSActionResultListener {
          override fun onSuccess() {
            Log.i(TAG, "Successfully sent change role request for $hmsPeer")
          }

          override fun onError(error: HMSException) {
            Log.e(TAG, "Error while sending change role request :: ${error.description}")
            state.postValue(MeetingState.NonFatalFailure(error))
          }
        })
      // Update the peer in participants
      peerLiveDate.postValue(hmsPeer!!)
    }
  }

  fun requestPeerLeave(hmsPeer: HMSRemotePeer, reason: String) {
    hmsSDK.removePeerRequest(hmsPeer, reason, object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        state.postValue(MeetingState.NonFatalFailure(error))
      }

      override fun onSuccess() {
        // Request Successfully sent to server
      }
    })
  }

  fun endRoom(lock: Boolean) {
    hmsSDK.endRoom("Closing time", lock, object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        state.postValue(MeetingState.NonFatalFailure(error))
      }

      override fun onSuccess() {
        // Request Successfully sent to server
        leaveMeeting()
      }
    })
  }

  fun togglePeerMute(hmsPeer: HMSRemotePeer, type: HMSTrackType) {

    val track = when(type) {
      HMSTrackType.AUDIO -> hmsPeer.audioTrack
      HMSTrackType.VIDEO -> hmsPeer.videoTrack
    }

    if (track != null) {
      val isMute = track.isMute
      if (isAllowedToAskUnmutePeers() && isMute) {
        hmsSDK.changeTrackState(track, false, object : HMSActionResultListener {
          override fun onError(error: HMSException) {
            state.postValue(MeetingState.NonFatalFailure(error))
          }

          override fun onSuccess() {
            // Request Successfully sent to server
          }

        })
      } else if (isAllowedToMutePeers() && !isMute) {
        hmsSDK.changeTrackState(track, true, object : HMSActionResultListener {
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

  fun getPeerForId(peerId: String): HMSPeer? {
    return hmsSDK.getPeers().find { it.peerID == peerId }
  }

  fun remoteMute(mute: Boolean, roles: List<String>?) {
    if (isAllowedToMutePeers()) {
      val selectedRoles = if (roles == null) null else {
        hmsSDK.getRoles().filter { roles.contains(it.name) }
      }
      hmsSDK.changeTrackState(mute, null, null, selectedRoles, object : HMSActionResultListener {
        override fun onError(error: HMSException) {
          Log.d(TAG, "remote mute Error $error")
        }

        override fun onSuccess() {
          Log.d(TAG, "remote mute Suceeded")
        }

      })
    }
  }

  /**
   * Returns true if audio tracks exist and are muted.
   * Returns false if audio tracks exist and are unmuted.
   * Returns null if no audio tracks or no remote peers exist.
   * Can check audio or video tracks. If nothing
   *  is specified it returns the || of audio mute and video mute
   */
  fun areAllRemotePeersMute(type: HMSTrackType? = null): Boolean? {
    val allPeerResults = hmsSDK.getRemotePeers().mapNotNull {
      when (type) {
        HMSTrackType.AUDIO -> {
          it.audioTrack?.isMute
        }
        HMSTrackType.VIDEO -> {
          it.videoTrack?.isMute
        }
        else -> {
          val audioMute = it.audioTrack?.isMute
          val videoMute = it.videoTrack?.isMute
          when {
            audioMute == null -> {
              videoMute
            }
            videoMute == null -> {
              audioMute
            }
            else -> {
              videoMute || audioMute
            }
          }
        }
      }
    }
    return if (allPeerResults.isEmpty()) {
      null
    } else {
      allPeerResults.reduce { acc, isMute -> acc && isMute }
    }
  }

  fun recordMeeting(
    isRecording: Boolean,
    rtmpInjectUrls: List<String>,
    meetingUrl: String,
    inputWidthHeight: HMSRtmpVideoResolution) {
    // It's streaming if there are rtmp urls present.

    Log.v(TAG, "Starting recording")
    hmsSDK.startRtmpOrRecording(
      HMSRecordingConfig(
        meetingUrl,
        rtmpInjectUrls,
        isRecording,
        inputWidthHeight
      ), object : HMSActionResultListener {
        override fun onError(error: HMSException) {
          Log.d(TAG, "RTMP recording error: $error")
          // restore the current state
          viewModelScope.launch { _events.emit(Event.RTMPError(error) ) }
        }

        override fun onSuccess() {
          Log.d(TAG, "RTMP recording Success")
        }

      })
  }

  fun stopRecording() {
    Log.v(TAG, "Stopping recording")

    hmsSDK.stopRtmpAndRecording(object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        Log.v(TAG, "RTMP recording stop. error: $error")
        viewModelScope.launch { _events.emit(Event.RTMPError(error)) }
      }

      override fun onSuccess() {
        Log.d(TAG, "RTMP recording stop. Success")
      }

    })
  }

  fun startScreenshare(mediaProjectionPermissionResultData: Intent?, actionListener: HMSActionResultListener) {
    // Without custom notification
//    hmsSDK.startScreenshare(actionListener ,mediaProjectionPermissionResultData)

    // With custom notification
    val notification = NotificationCompat.Builder(getApplication(), "ScreenCapture channel")
      .setContentText("Screenshare running for roomId: ${hmsRoom?.roomId}")
      .setSmallIcon(R.drawable.arrow_up_float)
      .addAction(R.drawable.ic_menu_close_clear_cancel, "Stop Screenshare", HMSScreenCaptureService.getStopScreenSharePendingIntent(getApplication()))
      .build()

    hmsSDK.startScreenshare(actionListener, mediaProjectionPermissionResultData, notification)
  }

  fun stopScreenshare(actionListener: HMSActionResultListener) {
    hmsSDK.stopScreenshare(actionListener)
  }

  private fun getRandomVirtualBackgroundBitmap(context: Context?) : Bitmap {

    val imageList = ArrayList<String>()
    imageList.add("2.jpeg")
    imageList.add("mountains.jpg")
    imageList.add("tree.jpg")
    imageList.add("4-3.jpg")
    imageList.add("16-9.jpg")
    imageList.add("720p.jpg")
    imageList.add("landscape.jpg")
    imageList.add("portrait.jpg")

    val randomIndex = Random.nextInt(imageList.size);
    return getBitmapFromAsset(context!!.applicationContext,imageList[randomIndex])!!
  }


  fun startVirtualBackgroundPlugin(context: Context?, actionListener: HMSActionResultListener) {
    Log.v(TAG, "Starting virtual background Plugin, First create a bitmap of the background required to be added")
    val imageBitmap = getRandomVirtualBackgroundBitmap(context)

    Log.v(TAG, "Add the bitmap to background")
    virtualBackgroundPlugin.setBackground(imageBitmap)

    hmsSDK.addPlugin(virtualBackgroundPlugin, actionListener)
  }

  fun stopVirtualBackgroundPlugin(actionListener: HMSActionResultListener) {
    Log.v(TAG, "Stopping virtual background Plugin")
    hmsSDK.removePlugin(virtualBackgroundPlugin, actionListener)
  }

  private val _events = MutableSharedFlow<Event?>()
  val events: SharedFlow<Event?> = _events

  sealed class Event {
    class RTMPError(val exception: HMSException) : Event()
    class ChangeTrackMuteRequest(val request: HMSChangeTrackStateRequest) : Event()
    object OpenChangeNameDialog : Event()
    sealed class Hls : Event() {
      data class HlsError(val throwable: HMSException) : Hls()
    }
    class HlsNotStarted(val reason : String) : Event()
    abstract class MessageEvent(open val message : String) : Event()
    data class RtmpEvent(val message : String) : Event()
    data class RecordEvent(val message : String) : Event()
    data class ServerRecordEvent(val message: String) : Event()
    data class HlsEvent(override val message : String) : MessageEvent(message)
    data class HlsRecordingEvent(override val message : String) : MessageEvent(message)
  }

  private val _isHandRaised = MutableLiveData<Boolean>(false)
  val isHandRaised: LiveData<Boolean> = _isHandRaised

  fun toggleRaiseHand() {
    val localPeer = hmsSDK.getLocalPeer()!!
    val currentMetadata = CustomPeerMetadata.fromJson(localPeer.metadata)
    val isHandRaised = currentMetadata!!.isHandRaised
    val newMetadataJson = currentMetadata.copy(isHandRaised = !isHandRaised).toJson()

    hmsSDK.changeMetadata(newMetadataJson, object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        Log.d(TAG, "There was an error $error")
      }

      override fun onSuccess() {
        Log.d(TAG, "Metadata update succeeded")
      }
    })

  }

  fun requestNameChange() {
    viewModelScope.launch {
      _events.emit(Event.OpenChangeNameDialog)
    }
  }

  fun changeName(name: String) {
    val localPeer = hmsSDK.getLocalPeer()!!
    hmsSDK.changeName(name, object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        Log.d(TAG, "There was an error $error")
      }

      override fun onSuccess() {
        Log.d(TAG, "Name update succeeded")
      }
    })
  }

  fun getStats(): Flow<Map<String, HMSStats>> = statsFlow

  fun startHls(hlsUrl : String, recordingConfig : HMSHlsRecordingConfig) {
    val config = HMSHLSConfig(listOf(HMSHLSMeetingURLVariant(hlsUrl)),
    recordingConfig)

    hmsSDK.startHLSStreaming(config, object : HMSActionResultListener {
      override fun onError(error: HMSException) {
        viewModelScope.launch {
            _events.emit(Event.Hls.HlsError(error))
        }
      }

      override fun onSuccess() {
        Log.d(TAG,"Hls streaming started successfully")
      }

    })
  }

  fun stopHls() {
    val config = HMSHLSConfig(emptyList())
    hmsSDK.stopHLSStreaming(config, object :  HMSActionResultListener {
      override fun onSuccess() {
        Log.d(TAG,"Hls streaming stopped successfully")
      }

      override fun onError(error: HMSException) {
        viewModelScope.launch {
          _events.emit(Event.Hls.HlsError(error))
        }

      }
    })
  }

  fun updateTrackStatus(status: String) {
    _trackStatus.value = status
  }

  var currentAudioMode = AudioManager.MODE_IN_COMMUNICATION

  fun toggleMediaMode()  {
    currentAudioMode = if (currentAudioMode == AudioManager.MODE_IN_COMMUNICATION) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
//    hmsSDK.setAudioMode(currentAudioMode)
  }

  fun getCurrentMediaModeCheckedState(): Boolean {
    return currentAudioMode != AudioManager.MODE_IN_COMMUNICATION
  }
}

