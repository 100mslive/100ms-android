package live.hms.roomkit.ui.meeting

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
//import live.hms.hls_player.HmsHlsPlayer
import live.hms.roomkit.R
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.meeting.activespeaker.ActiveSpeakerHandler
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.meeting.participants.ParticipantPreviousRoleChangeUseCase
import live.hms.roomkit.ui.notification.HMSNotification
import live.hms.roomkit.ui.notification.HMSNotificationType
import live.hms.roomkit.ui.polls.PollCreationInfo
import live.hms.roomkit.ui.polls.QuestionUi
import live.hms.roomkit.ui.settings.SettingsFragment.Companion.REAR_FACING_CAMERA
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.util.POLL_IDENTIFIER_FOR_HLS_CUE
import live.hms.roomkit.util.SingleLiveEvent
import live.hms.video.audio.HMSAudioManager
import live.hms.video.connection.stats.*
import live.hms.video.error.HMSException
import live.hms.video.interactivity.HmsInteractivityCenter
import live.hms.video.interactivity.HmsPollUpdateListener
import live.hms.video.events.AgentType
import live.hms.video.factories.noisecancellation.AvailabilityStatus
import live.hms.video.media.settings.*
import live.hms.video.media.tracks.*
import live.hms.video.polls.HMSPollBuilder
import live.hms.video.polls.HMSPollQuestionBuilder
import live.hms.video.polls.HMSPollResponseBuilder
import live.hms.video.polls.models.HMSPollUpdateType
import live.hms.video.polls.models.HmsPoll
import live.hms.video.polls.models.HmsPollCategory
import live.hms.video.polls.models.HmsPollState
import live.hms.video.polls.models.answer.PollAnswerResponse
import live.hms.video.polls.models.network.HMSPollQuestionResponse
import live.hms.video.polls.models.question.HMSPollQuestion
import live.hms.video.polls.models.question.HMSPollQuestionType
import live.hms.video.polls.network.PollLeaderboardResponse
import live.hms.video.sdk.*
import live.hms.video.sdk.models.*
import live.hms.video.sdk.models.enums.*
import live.hms.video.sdk.models.role.HMSRole
import live.hms.video.sdk.models.trackchangerequest.HMSChangeTrackStateRequest
import live.hms.video.services.HMSScreenCaptureService
import live.hms.video.services.LogAlarmManager
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.signal.init.*
import live.hms.video.utils.HMSLogger
import live.hms.videofilters.HMSVideoFilter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates


class MeetingViewModel(
    application: Application
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MeetingViewModel"
    }

    val launchParticipantsFromHls = SingleLiveEvent<Unit>()
    var recNum = 0
    // This is needed in chat for it to determine what kind of chat it is.
    val initPrebuiltChatMessageRecipient = MutableLiveData<Pair<Recipient?,Int>>()
    val audioDeviceChange = MutableLiveData<HMSAudioManager.AudioDevice>()
    val participantPreviousRoleChangeUseCase by lazy { ParticipantPreviousRoleChangeUseCase(hmsSDK::changeMetadata)}
    private var hasValidToken = false
    private var pendingRoleChange: HMSRoleChangeRequest? = null
    private var hmsRoomLayout : HMSRoomLayout? = null
    val prebuiltInfoContainer by lazy { PrebuiltInfoContainer(hmsSDK) }
    val toggleNcInPreview : MutableLiveData<Boolean> = MutableLiveData(false)

    private val settings = SettingsStore(getApplication())
    private val hmsLogSettings: HMSLogSettings =
        HMSLogSettings(LogAlarmManager.DEFAULT_DIR_SIZE, true)
    private var isPrebuiltDebug by Delegates.notNull<Boolean>()
    val roleChange = MutableLiveData<HMSPeer>()

    var roleOnJoining : HMSRole? = null
        private set

    var localPeerId : String? = null
        private set

    var roomLogoUrl : String? = null
    var liveClassName : String? = null
    var isLiveIconEnabled : Boolean? = null
    var isRecordingIconsEnabled : Boolean? = null

    fun isLargeRoom() = hmsRoom?.isLargeRoom?:false

    private val hmsTrackSettings = HMSTrackSettings.Builder()
        .audio(
            HMSAudioTrackSettings.Builder()
                .setUseHardwareAcousticEchoCanceler(settings.enableHardwareAEC)
                .initialState(getAudioTrackState())
                .setDisableInternalAudioManager(settings.detectDominantSpeaker.not())
                .setPhoneCallMuteState(if (settings.muteLocalAudioOnPhoneRing) PhoneCallState.ENABLE_MUTE_ON_PHONE_CALL_RING else PhoneCallState.DISABLE_MUTE_ON_VOIP_PHONE_CALL_RING)
                .build()
        )
        .video(
            HMSVideoTrackSettings.Builder().disableAutoResize(settings.disableAutoResize)
                .forceSoftwareDecoder(settings.forceSoftwareDecoder)
                .setDegradationPreference(settings.degradationPreferences)
                .initialState(getVideoTrackState())
                .cameraFacing(getVideoCameraFacing())
                .build()
        )
        .build()

    val hmsSDK = HMSSDK
        .Builder(application)
        .haltPreviewJoinForPermissionsRequest(true)
        .setFrameworkInfo(FrameworkInfo(framework = AgentType.ANDROID_NATIVE, isPrebuilt = true))
        .setTrackSettings(hmsTrackSettings) // SDK uses HW echo cancellation, if nothing is set in builder
        .setLogSettings(hmsLogSettings)
        .build()


    val filterPlugin  by lazy { HMSVideoFilter(hmsSDK) }

    private var lastPollStartedTime : Long = 0

    val localHmsInteractivityCenter : HmsInteractivityCenter = hmsSDK.getHmsInteractivityCenter()
        .apply {
            this.pollUpdateListener = object : HmsPollUpdateListener {
                override fun onPollUpdate(
                    hmsPoll: HmsPoll,
                    hmsPollUpdateType: HMSPollUpdateType
                ) {
                    when(hmsPollUpdateType) {
                        HMSPollUpdateType.started -> viewModelScope.launch {
                            if(!isHlsPeer(hmsSDK.getLocalPeer()?.hmsRole)) {
                                _events.emit(Event.PollStarted(hmsPoll))
                                // Only show latest polls
                                if (lastPollStartedTime < hmsPoll.startedAt) {
                                    lastPollStartedTime = hmsPoll.startedAt
                                    triggerPollsNotification(hmsPoll)

                                }
                            }
                        }
                        HMSPollUpdateType.stopped -> viewModelScope.launch {
                            _events.emit(Event.PollEnded(hmsPoll))
                            hmsRemoveNotificationEvent.postValue(HMSNotificationType.OpenPollOrQuiz(pollId = hmsPoll.pollId))
                        }
                        HMSPollUpdateType.resultsupdated -> viewModelScope.launch {
                            _events.emit(Event.PollVotesUpdated(hmsPoll))
                        }
                    }
                }

            }
        }
    fun getHmsRoomLayout() = hmsRoomLayout

    var prebuiltOptions : HMSPrebuiltOptions? = null
    fun initSdk(
        roomCode: String,
        token: String,
        hmsPrebuiltOptions: HMSPrebuiltOptions?,
        onHMSActionResultListener: HMSActionResultListener?
    ) {
        this.prebuiltOptions = hmsPrebuiltOptions
        if (hasValidToken) {
            onHMSActionResultListener?.onSuccess()
            roomLayoutLiveData.postValue(true)
            return
        }
        //if empty is uses the prod token url else uses the debug token url
        val tokenURL: String = hmsPrebuiltOptions?.endPoints?.get("token") ?: ""


        isPrebuiltDebug = hmsPrebuiltOptions?.debugInfo ?: false

        if (token.isNullOrEmpty().not()) {
            joinRoomUsingToken(token, hmsPrebuiltOptions, onHMSActionResultListener)
            return
        }



        hmsSDK.getAuthTokenByRoomCode(
            TokenRequest(roomCode, hmsPrebuiltOptions?.userId ?: UUID.randomUUID().toString()),
            TokenRequestOptions(tokenURL),
            object : HMSTokenListener {
                override fun onError(error: HMSException) {
                    hasValidToken = false
                    onHMSActionResultListener?.onError(error)
                    roomLayoutLiveData.postValue(false)
                }

                override fun onTokenSuccess(token: String) {
                    joinRoomUsingToken(token, hmsPrebuiltOptions, onHMSActionResultListener)
                }

            })
    }

    fun showVideoFilterIcon() = settings.enableVideoFilter

     fun setupFilterVideoPlugin() {

        if (hmsSDK.getPlugins().isNullOrEmpty() && hmsSDK.getLocalPeer()?.videoTrack != null ) {
            filterPlugin.init()
            hmsSDK.addPlugin(filterPlugin, object : HMSActionResultListener {
                override fun onError(error: HMSException) {

                }

                override fun onSuccess() {

                }

            }, 30)
        }
    }

    fun removeVideoFilterPlugIn() {

        if (hmsSDK.getPlugins().isNullOrEmpty().not() ) {
            filterPlugin.stop()
            hmsSDK.removePlugin(filterPlugin, object : HMSActionResultListener {
                override fun onError(error: HMSException) {

                }

                override fun onSuccess() {

                }

            })
        }

    }


    fun joinRoomUsingToken(token: String, hmsPrebuiltOptions: HMSPrebuiltOptions?, onHMSActionResultListener: HMSActionResultListener?) {

        val initURL: String = if (hmsPrebuiltOptions?.endPoints?.containsKey("init") == true)
            hmsPrebuiltOptions.endPoints["init"].orEmpty()
        else
            "https://prod-init.100ms.live/init"

        val layoutEndpointBase = hmsPrebuiltOptions?.endPoints?.get("layout")
        hmsSDK.getRoomLayout(
            token,
            LayoutRequestOptions(layoutEndpointBase),
            object :
                HMSLayoutListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, "onError: ", error)
                    onHMSActionResultListener?.onError(error)
                    roomLayoutLiveData.postValue(false)
                }

                override fun onLayoutSuccess(layoutConfig: HMSRoomLayout) {
                    hmsRoomLayout = layoutConfig
                    prebuiltInfoContainer.setParticipantLabelInfo(hmsRoomLayout)
                    Log.d("Pratim", "Setting HMS Config")
                    setHmsConfig(hmsPrebuiltOptions, token, initURL)
                    kotlin.runCatching { setTheme(layoutConfig.data?.getOrNull(0)?.themes?.getOrNull(0)?.palette!!) }
                    onHMSActionResultListener?.onSuccess()
                    roomLayoutLiveData.postValue(true)
                }

            })

    }

    private fun setTheme(theme: HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette) {
        HMSPrebuiltTheme.setTheme(theme)
    }

    fun updateNameInPreview(nameStr: String) {
            hmsConfig = hmsConfig?.copy(userName = nameStr)
    }

    private fun setHmsConfig(
        hmsPrebuiltOptions: HMSPrebuiltOptions?,
        token: String,
        initURL: String
    ) {
        hmsConfig = HMSConfig(
            userName = hmsPrebuiltOptions?.userName ?: UUID.randomUUID().toString(),
            token,
            Gson().toJson(
                CustomPeerMetadata(
                    isHandRaised = false,
                    name = hmsPrebuiltOptions?.userName.orEmpty(),
                    isBRBOn = false
                )
            )
                .toString(),
            captureNetworkQualityInPreview = true,
            initEndpoint = initURL,
        )

        hasValidToken = true
    }

    private var hmsConfig: HMSConfig? = null
    private val recordingTimesUseCase = RecordingTimesUseCase()

    private fun showServerInfo(room: HMSRoom) {
        viewModelScope.launch {
            _events.emit(Event.ServerRecordEvent(recordingTimesUseCase.showServerInfo(room)))
        }
    }

    private fun showRecordInfo(room: HMSRoom) {
        viewModelScope.launch {
            _events.emit(Event.RecordEvent(recordingTimesUseCase.showRecordInfo(room)))
        }
    }

    private fun showRtmpInfo(room: HMSRoom) {
        viewModelScope.launch {
            _events.emit(Event.RtmpEvent(recordingTimesUseCase.showRtmpInfo(room)))
        }
    }

    private fun showHlsInfo(room: HMSRoom) {
        viewModelScope.launch {
            _events.emit(Event.HlsEvent(recordingTimesUseCase.showHlsInfo(room, false)))
        }
    }

    private fun showHlsRecordingInfo(room: HMSRoom) {
        viewModelScope.launch {
            _events.emit(Event.HlsRecordingEvent(recordingTimesUseCase.showHlsInfo(room, true)))
        }
    }

    private val _tracks = Collections.synchronizedList(ArrayList<MeetingTrack>())

    // When we get stats, a flow will be updated with the saved stats.
    private val statsFlow = MutableSharedFlow<Map<String, Any>>()
    private val savedStats: MutableMap<String, Any> = mutableMapOf()


    private val failures = ArrayList<HMSException>()

    val meetingViewMode = MutableLiveData(settings.meetingMode)

    private val roomState: MutableLiveData<Pair<HMSRoomUpdate, HMSRoom>> = MutableLiveData()
    private val previewPeerData: MutableLiveData<Pair<HMSPeerUpdate, HMSPeer>> = MutableLiveData()
    private val previewErrorData: MutableLiveData<HMSException> = MutableLiveData()
    private val previewUpdateData: MutableLiveData<Pair<HMSRoom, Array<HMSTrack>>> =
        MutableLiveData()
    val statsToggleData: MutableLiveData<Boolean> = MutableLiveData(settings.showStats)
    val peerCount = MutableLiveData(0)

    val previewRoomStateLiveData: LiveData<Pair<HMSRoomUpdate, HMSRoom>> = roomState
    val previewPeerLiveData: LiveData<Pair<HMSPeerUpdate, HMSPeer>> = previewPeerData
    val previewErrorLiveData: LiveData<HMSException> = previewErrorData
    val previewUpdateLiveData: LiveData<Pair<HMSRoom, Array<HMSTrack>>> = previewUpdateData
    val statsToggleLiveData: LiveData<Boolean> = statsToggleData
    val isScreenShare: MutableLiveData<Boolean>  = MutableLiveData(false)
    val hmsNotificationEvent = SingleLiveEvent<HMSNotification>()
    val hmsRemoveNotificationEvent = MutableLiveData<HMSNotificationType>()
    val updateGridLayoutDimensions = SingleLiveEvent<Boolean>()
    val hmsScreenShareBottomSheetEvent = SingleLiveEvent<String>()
    val roomLayoutLiveData : MutableLiveData<Boolean> = MutableLiveData()

    fun setMeetingViewMode(mode: MeetingViewMode) {
        if (mode != meetingViewMode.value) {
            meetingViewMode.postValue(mode)
        }
    }

    fun isAutoSimulcastEnabled() = settings.disableAutoSimulcast

    fun isGoLiveInPreBuiltEnabled() = settings.enableVideoFilter

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

    private var hmsRoom: HMSRoom? = null

    // Live data for enabling/disabling mute buttons
    val isLocalAudioPresent = MutableLiveData(false)
    val isLocalVideoPresent = MutableLiveData(false)

    //Live data to show ui for recording and streaming states
    val recordingState = MutableLiveData(HMSRecordingState.NONE)
    val streamingState = MutableLiveData(HMSStreamingState.NONE)

    // Live data containing all the current tracks in a meeting
    private val _liveDataTracks = MutableLiveData(_tracks)
    val tracks: LiveData<List<MeetingTrack>> = _liveDataTracks

    // Live data containing the current Speaker in the meeting
    val speakers = MutableLiveData<Array<HMSSpeaker>>()

    private val activeSpeakerHandler = ActiveSpeakerHandler(false) { _tracks }

    val updateRowAndColumnSpanForVideoPeerGrid = MutableLiveData<Pair<Int,Int>>()

    val speakerUpdateLiveData = object : ActiveSpeakerLiveData() {
        private val speakerH = ActiveSpeakerHandler(true,settings.videoGridRows* settings.videoGridColumns
        ) { _tracks }

        override fun addSpeakerSource() {
            addSource(speakers) { speakers : Array<HMSSpeaker> ->

                val excludeLocalTrackIfRemotePeerIsPreset : Array<HMSSpeaker> = if (hasInsetEnabled(hmsSDK.getLocalPeer()?.hmsRole)) {
                    speakers.filter { it.peer?.isLocal == false }.toTypedArray()
                } else {
                    speakers
                }

                val result = speakerH.speakerUpdate(excludeLocalTrackIfRemotePeerIsPreset)
                setValue(result.first)
            }
        }

        override fun removeSpeakerSource() {
            removeSource(speakers)
        }

        //TODO can't be null
        fun refreshSpeaker() {
           // speakers.postValue(speakers.value)
        }

        override fun updateMaxActiveSpeaker(rowCount: Int, columnCount: Int) {
            speakerH.updateMaxActiveSpeaker(rowCount*columnCount)
            refreshSpeaker()
        }
        init {
            addSpeakerSource()

            // Add all tracks as they come in.
            addSource(tracks) { meetTracks: List<MeetingTrack> ->
                //if remote peer and local peer is present inset mode
               synchronized(_tracks) {
                   val excludeLocalTrackIfRemotePeerIsPreset =
                       //Don't inset when local peer and local screen share track is found
                       if (meetTracks.size == 2 && meetTracks.filter { it.isLocal }.size == 2 && hasInsetEnabled(
                               hmsSDK.getLocalPeer()?.hmsRole
                           )
                       )
                           meetTracks
                       else if (meetTracks.size > 1 && hasInsetEnabled(hmsSDK.getLocalPeer()?.hmsRole))
                           meetTracks.filter { !it.isLocal }.toList()
                       else
                           meetTracks

                   val result =
                       speakerH.trackUpdateTrigger(excludeLocalTrackIfRemotePeerIsPreset.filter { it.isScreen.not() })
                   setValue(result)
               }

            }

        }



    }

    val activeSpeakers: LiveData<Pair<List<MeetingTrack>, Array<HMSSpeaker>>> =
        speakers.map(activeSpeakerHandler::speakerUpdate)
    val activeSpeakersUpdatedTracks = _liveDataTracks.map(activeSpeakerHandler::trackUpdateTrigger)

    // We need all the active speakers, but the very first time it should be filled.
    //  with all the tracks.
    //  subsequent updates that matter are:
    //  track updates (which can add or remove tracks at the end)
    //  Active speaker updates which modify the current ones.

    // Live data which changes on any change of peer
    val peerLiveData = MutableLiveData<HMSPeer>()
    val participantPeerUpdate = MutableLiveData<Unit>()
    val peerLeaveUpdate = MutableLiveData<String?>(null)
    private val _peerMetadataNameUpdate = MutableLiveData<Pair<HMSPeer, HMSPeerUpdate>>()
    val peerMetadataNameUpdate: LiveData<Pair<HMSPeer, HMSPeerUpdate>> = _peerMetadataNameUpdate

    // Dominant speaker is for active speaker as well as pinned tracks.
    private val dominantSpeaker = MutableLiveData<MeetingTrack?>(null)
    val pinnedTrack = MutableLiveData<MeetingTrack?>(null)
    val localPinnedTrack = MutableLiveData<MeetingTrack?>(null)

    val pinnedTrackUiUseCase = PinnedTrackUiUseCase(
        local = localPinnedTrack,
        global = pinnedTrack
    )


    val broadcastsReceived = MutableLiveData<ChatMessage>()

    private val _trackStatus = MutableLiveData<Pair<String, Boolean>>()
    val trackStatus: LiveData<Pair<String, Boolean>> = _trackStatus


    val peers: List<HMSPeer>
        get() = hmsSDK.getPeers()
//    val permissionCompletable = CompletableDeferred<Boolean>()

    fun startPreview() {
        if (hmsConfig == null) {
            HMSLogger.e(TAG, "HMSConfig is null. Cannot start preview.")
            return
        }

        // call Preview api
        hmsSDK.preview(hmsConfig!!, object : HMSPreviewListener {
            override fun onError(error: HMSException) {
                previewErrorData.postValue(error)
            }

            override fun onPermissionsRequested(permissions : List<String>) {
                viewModelScope.launch {
                    _events.emit(Event.RequestPermission(permissions.toTypedArray()))
                }
            }

            override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
                previewPeerData.postValue(Pair(type, peer))
            }

            override fun onPreview(room: HMSRoom, localTracks: Array<HMSTrack>) {
                Log.d("Pratim", "onPreview called")
                unMuteAllTracks(localTracks)
                previewUpdateData.postValue(Pair(room, localTracks))

            }

            override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
                roomState.postValue(Pair(type, hmsRoom))

                if (type == HMSRoomUpdate.ROOM_PEER_COUNT_UPDATED) {
                    peerCount.postValue(hmsRoom.peerCount)
                }
            }

        })
    }

    private fun unMuteAllTracks(localTracks: Array<HMSTrack>) {
        localTracks.forEach {
            if (it is HMSLocalVideoTrack)
                it.setMute(false)
            else if(it is HMSLocalAudioTrack)
                it.setMute(false)
        }
    }

    fun setLocalVideoEnabled(enabled: Boolean) {

        hmsSDK.getLocalPeer()?.videoTrack?.apply {

            setMute(!enabled)

            _liveDataTracks.postValue(_tracks)

            isLocalVideoEnabled.postValue(enabled)
        }
    }

    private fun getAudioTrackState() =
        if (settings.isAudioTrackInitStateEnabled.not()) HMSTrackSettings.InitState.MUTED else HMSTrackSettings.InitState.UNMUTED

    private fun getVideoTrackState() =
        if (settings.isVideoTrackInitStateEnabled.not()) HMSTrackSettings.InitState.MUTED else HMSTrackSettings.InitState.UNMUTED

    private fun getVideoCameraFacing() =
        if (settings.camera.contains(REAR_FACING_CAMERA)) HMSVideoTrackSettings.CameraFacing.BACK else HMSVideoTrackSettings.CameraFacing.FRONT


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
        }

    }

    fun isLocalAudioEnabled(): Boolean? {
        return hmsSDK.getLocalPeer()?.audioTrack?.isMute?.not()
    }

    fun toggleLocalAudio() {
        //hmsNotificationEvent.postValue(HMSNotification(title = "Test ${System.currentTimeMillis()} ", icon = R.drawable.person_icon, isDismissible = true))
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
                Log.d("RtcStatsObserver", "Local VideoStats: $audioStats")
                val id = hmsTrack?.trackId
                if (id != null)
                    savedStats[id] = audioStats
            }

            override fun onLocalVideoStats(
                videoStats: List<HMSLocalVideoStats>,
                hmsTrack: HMSTrack?,
                hmsPeer: HMSPeer?
            ) {
                Log.d("RtcStatsObserver", "Local VideoStats: $videoStats")
                val id = hmsTrack?.trackId
                if (id != null)
                    savedStats[id] = videoStats

            }

            override fun onRTCStats(rtcStats: HMSRTCStatsReport) {
                Log.d("RtcStatsObserver", "Cumulative stats: $rtcStats")
                viewModelScope.launch {
                    statsFlow.emit(savedStats)
                }
            }

            override fun onRemoteAudioStats(
                audioStats: HMSRemoteAudioStats,
                hmsTrack: HMSTrack?,
                hmsPeer: HMSPeer?
            ) {
                Log.d(
                    "RtcStatsObserver",
                    "Remote audio stats: $audioStats for peer : ${hmsPeer?.name}, with track : ${hmsTrack?.trackId}"
                )
                val id = hmsTrack?.trackId
                if (id != null)
                    savedStats[id] = audioStats
            }

            override fun onRemoteVideoStats(
                videoStats: HMSRemoteVideoStats,
                hmsTrack: HMSTrack?,
                hmsPeer: HMSPeer?
            ) {
                Log.d(
                    "RtcStatsObserver",
                    "Remote video stats: $videoStats for peer : ${hmsPeer?.name}, with track : ${hmsTrack?.trackId}"
                )
                val id = hmsTrack?.trackId
                if (id != null)
                    savedStats[id] = videoStats
            }

        })
    }

    fun setRtcObserver(isEnabled: Boolean) {
        if (isEnabled) {
            addRTCStatsObserver()
        } else {
            removeRtcStatsObserver()
        }
        statsToggleData.postValue(isEnabled)
    }

    fun removeRtcStatsObserver() {
        hmsSDK.removeRtcStatsObserver()
    }

    fun startMeeting() {

        if (hmsConfig == null) {
            HMSLogger.e(TAG, "HMSConfig is null. Cannot start preview.")
            return
        }

        if (settings.showStats) {
            addRTCStatsObserver()
        }

        

        cleanup()

        state.postValue(
            MeetingState.Connecting(
                "Connecting",
                "Establishing websocket connection"
            )
        )

        val joinStartedAt = System.currentTimeMillis()
        Log.v(TAG, "~~ hmsSDK.join called ~~")
        hmsSDK.join(hmsConfig!!, object : HMSUpdateListener {

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

            override fun onPermissionsRequested(permissions: List<String>) {
                viewModelScope.launch {
                    _events.emit(Event.RequestPermission(permissions.toTypedArray()))
                }
            }

            override fun onSessionStoreAvailable(sessionStore: HmsSessionStore) {
                super.onSessionStoreAvailable(sessionStore)
                sessionMetadataUseCase.setSessionStore(sessionStore)
                pinnedTrackUseCase = PinnedTrackUseCase(sessionStore)
                blockUserUseCase.setSessionStore(sessionStore)
                hideMessageUseCase.setSessionStore(sessionStore)
                pauseChatUseCase.setSessionStore(sessionStore)
            }

            override fun onJoin(room: HMSRoom) {
                Log.v(TAG, "~~ onJoin called ~~")
                val joinSuccessAt = System.currentTimeMillis();
                val timeTakenToJoin = joinSuccessAt - joinStartedAt
                Log.d(TAG, "~~ HMS SDK took $timeTakenToJoin ms to join ~~")
                failures.clear()
                state.postValue(MeetingState.Ongoing())
                hmsRoom = room // Just storing the room id for the beam bot.
                Log.d(TAG, "$room")
                Log.d(TAG, "Room name is ${room.name}")
                Log.d(TAG, "SessionId is: ${room.sessionId}")
                Log.d(TAG, "Room started at: ${room.startedAt}")
                roleOnJoining = room.localPeer?.hmsRole
                localPeerId = room.localPeer?.peerID

                // get the hls URL from the Room, if it exists
                val hlsUrl = room.hlsStreamingState.variants?.get(0)?.hlsStreamUrl
                switchToHlsViewIfRequired(room.localPeer?.hmsRole, hlsUrl)

                val runningStreamingStates = listOf(HMSStreamingState.STARTED, HMSStreamingState.STARTING)
                val runningRecordingStates = listOf(HMSRecordingState.STARTING, HMSRecordingState.STARTED, HMSRecordingState.PAUSED, HMSRecordingState.RESUMED)

                if(toggleNcInPreview.value == true) {
                    hmsSDK.setNoiseCancellationEnabled(true)
                }
                if (room.hlsStreamingState.state in runningStreamingStates)
                    streamingState.postValue(room.hlsStreamingState.state)
                if (room.rtmpHMSRtmpStreamingState.state in runningStreamingStates)
                    streamingState.postValue(room.rtmpHMSRtmpStreamingState.state)
                if (room.browserRecordingState.state in runningRecordingStates)
                    recordingState.postValue(room.browserRecordingState.state)
                if (room.hlsRecordingState.state in runningRecordingStates )
                    recordingState.postValue(room.hlsRecordingState.state)
                sessionMetadataUseCase.updatePeerName(room.localPeer?.name ?: "Participant")
                initPrebuiltChatMessageRecipient.postValue(Pair(prebuiltInfoContainer.defaultRecipientToMessage(), ++recNum))
                sessionMetadataUseCase.setPinnedMessageUpdateListener(
                    object : HMSActionResultListener {
                        override fun onError(error: HMSException) {}
                        override fun onSuccess() {}
                    }
                )
                pinnedTrackUseCase.setPinnedTrackListener(
                    { trackId ->
                        if (trackId == null) {
                            pinnedTrack.postValue(null)
                        } else {
                            getMeetingTrack(trackId)?.let { pinnedTrack.postValue(it) }
                        }
                    },
                    object : HMSActionResultListener {
                        override fun onError(error: HMSException) {}
                        override fun onSuccess() {}
                    }
                )
                blockUserUseCase.addKeyChangeListener()
                pauseChatUseCase.addKeyChangeListener()
                hideMessageUseCase.addKeyChangeListener()
                updatePolls()
                participantPeerUpdate.postValue(Unit)
            }

            override fun onPeerUpdate(type: HMSPeerUpdate, hmsPeer: HMSPeer) {
                Log.d(TAG, "join:onPeerUpdate type=$type, peer=$hmsPeer")

                when (type) {
                    HMSPeerUpdate.PEER_LEFT -> {
                        synchronized(_tracks) {
                            for (track in _tracks) {
                                if (track.peer.peerID == hmsPeer.peerID) {
                                    _tracks.remove(track)
                                    break
                                }
                            }
                            _liveDataTracks.postValue(_tracks)
                            peerLiveData.postValue(hmsPeer)
                        }
                        participantPeerUpdate.postValue(Unit)
                        peerLeaveUpdate.postValue(hmsPeer.peerID)
                    }

                    HMSPeerUpdate.PEER_JOINED -> {
                        peerLiveData.postValue(hmsPeer)
                        participantPeerUpdate.postValue(Unit)
                    }

                    HMSPeerUpdate.BECAME_DOMINANT_SPEAKER -> {
                        synchronized(_tracks) {
                            val track = getMeetingTrack(hmsPeer.videoTrack?.trackId)
                            if (track != null) {
                                Log.d(TAG, "Getting local dominant speaker ${track.peer.name}")
                                dominantSpeaker.postValue(track)
                            }
                        }
                    }

                    HMSPeerUpdate.NO_DOMINANT_SPEAKER -> {
                        dominantSpeaker.postValue(null)
                    }

                    HMSPeerUpdate.ROLE_CHANGED -> {
                        Log.d(
                            "RoleChangeUpdate",
                            "${hmsPeer.name} changed to ${hmsPeer.hmsRole.name}"
                        )
                        if(hmsPeer.isLocal && type == HMSPeerUpdate.ROLE_CHANGED) {
                            // Changed on a force change. This will happen twice.
                            // when a person is brought to offstage
                            participantPreviousRoleChangeUseCase.setPreviousRole(
                                hmsSDK.getLocalPeer()!!,
                                roleOnJoining?.name,
                                object : HMSActionResultListener {
                                    override fun onError(error: HMSException) {}
                                    override fun onSuccess() {}
                                })

                            initPrebuiltChatMessageRecipient.postValue(Pair(prebuiltInfoContainer.defaultRecipientToMessage(), ++recNum))
                            roleChange.postValue(hmsPeer)
                        }
                        peerLiveData.postValue(hmsPeer)
                        if (hmsPeer.isLocal) {
                            // get the hls URL from the Room, if it exists
                            updateThemeBasedOnCurrentRole(hmsPeer.hmsRole)
                            val hlsUrl = hmsRoom?.hlsStreamingState?.variants?.get(0)?.hlsStreamUrl
                            val isHlsPeer = isHlsPeer(hmsPeer.hmsRole)
                            showAudioIcon.postValue(!isHlsPeer)
                            if (isHlsPeer) {
                                switchToHlsViewIfRequired(hmsPeer.hmsRole, hlsUrl)
                            } else {
                                showHlsStreamYetToStartError.postValue(false)
                                exitHlsViewIfRequired(false)
                            }

                        }

                        participantPeerUpdate.postValue(Unit)
                    }

                    HMSPeerUpdate.METADATA_CHANGED ,
                    HMSPeerUpdate.HAND_RAISED_CHANGED -> {
                        if (type == HMSPeerUpdate.HAND_RAISED_CHANGED)
                            triggerBringOnStageNotificationIfHandRaised(hmsPeer)
                        if (hmsPeer.isLocal) {
                            updateSelfHandRaised(hmsPeer as HMSLocalPeer)
                        } else {
                            _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
                        }
                        participantPeerUpdate.postValue(Unit)
                    }

                    HMSPeerUpdate.NAME_CHANGED -> {
                        if (hmsPeer.isLocal) {
                            updateNameChange(hmsPeer as HMSLocalPeer)
                        } else {
                            _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
                        }
                        participantPeerUpdate.postValue(Unit)
                    }

                    HMSPeerUpdate.NETWORK_QUALITY_UPDATED -> {
                        _peerMetadataNameUpdate.postValue(Pair(hmsPeer, type))
                        participantPeerUpdate.postValue(Unit)
                    }
                    else -> Unit
                }
            }

            override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
                Log.d(TAG, "join:onRoomUpdate type=$type, room=$hmsRoom")

                when (type) {
                    HMSRoomUpdate.ROOM_PEER_COUNT_UPDATED -> {
                        peerCount.postValue(hmsRoom.peerCount)
                    }

                    HMSRoomUpdate.SERVER_RECORDING_STATE_UPDATED -> {
                        showServerInfo(hmsRoom)
                    }

                    HMSRoomUpdate.RTMP_STREAMING_STATE_UPDATED -> {
                        showRtmpInfo(hmsRoom)
                        streamingState.postValue(hmsRoom.rtmpHMSRtmpStreamingState.state)
                    }

                    HMSRoomUpdate.BROWSER_RECORDING_STATE_UPDATED -> {
                        showRecordInfo(hmsRoom)
                        recordingState.postValue(hmsRoom.browserRecordingState.state)
                    }

                    HMSRoomUpdate.HLS_STREAMING_STATE_UPDATED -> {
                        switchToHlsViewIfRequired()
                        showHlsInfo(hmsRoom)
                        streamingState.postValue(hmsRoom.hlsStreamingState.state)
                    }

                    HMSRoomUpdate.HLS_RECORDING_STATE_UPDATED -> {
                        showHlsRecordingInfo(hmsRoom)
                        recordingState.postValue(hmsRoom.hlsRecordingState.state)
                    }

                    HMSRoomUpdate.ROOM_MUTED -> {

                    }
                    HMSRoomUpdate.ROOM_UNMUTED -> {

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
                                    isLocalAudioPresent.postValue(true)
                                    isLocalAudioEnabled.postValue(!track.isMute)
                                }

                                HMSTrackType.VIDEO -> {
                                    isLocalVideoPresent.postValue(true)
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
                                    isLocalAudioPresent.postValue(false)
                                }

                                HMSTrackType.VIDEO -> {
                                    isLocalVideoPresent.postValue(false)
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
                participantPeerUpdate.postValue(Unit)
            }

            override fun peerListUpdated(
                addedPeers: ArrayList<HMSPeer>?,
                removedPeers: ArrayList<HMSPeer>?
            ) {
                Log.d(TAG, "peerListUpdated - added peers = $addedPeers, removed peers = $removedPeers")
            }

            override fun onMessageReceived(message: HMSMessage) {
                Log.v(TAG, "onMessageReceived: $message")
                if(message.type != HMSMessageType.CHAT)
                    return
                broadcastsReceived.postValue(

                    ChatMessage(
                        message, false,
                        message.recipient.recipientPeer?.peerID == localPeerId
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

    private fun triggerBringOnStageNotificationIfHandRaised(handRaisedPeer: HMSPeer) {
        //triggerd user has on stage role permission
        if(hasBringOnStageRolePermission(currentRole = hmsSDK.getLocalPeer()?.hmsRole, handRaisedPeerRole = handRaisedPeer.hmsRole)
            && hmsSDK.getLocalPeer()?.hmsRole?.name == "broadcaster"
            && getOnStageRole(hmsSDK.getLocalPeer()?.hmsRole).isNullOrEmpty().not()
        ) {
            if (handRaisedPeer.isHandRaised) {
                hmsNotificationEvent.postValue(
                    HMSNotification(
                        title = "${handRaisedPeer.name} raised hand",
                        isDismissible = false,
                        icon = R.drawable.hand_raised,
                        actionButtonText = "Bring on stage",
                        type = HMSNotificationType.BringOnStage(handRaisedPeer, getOnStageRole(hmsSDK.getLocalPeer()?.hmsRole).orEmpty())
                    )
                )
            }
        }

    }

     fun triggerPollsNotification(poll: HmsPoll) {

         val res = getApplication<Application>().resources
         val pollOrQuiz = res.getString(if (poll.category == HmsPollCategory.POLL) R.string.hms_poll else R.string.hms_quiz)
         val actionButtonText = res.getString(if (poll.category == HmsPollCategory.POLL) R.string.hms_vote else R.string.hms_answer)

        hmsNotificationEvent.postValue(
            HMSNotification(
                title = res.getString(R.string.hms_started_quiz_poll_notification, poll.createdBy?.name.orEmpty(), pollOrQuiz),
                isDismissible = true,
                icon = R.drawable.poll_vote,
                actionButtonText = actionButtonText,
                type = HMSNotificationType.OpenPollOrQuiz(pollId = poll.pollId)
            )
        )

    }

    private var lastStartedPoll : HmsPoll? = null
    private fun updatePolls() {
        // Just running this is enough since it will trigger the poll started notifications
        localHmsInteractivityCenter.fetchPollList(HmsPollState.STARTED, object : HmsTypedActionResultListener<List<HmsPoll>>{
            override fun onSuccess(result: List<HmsPoll>) {
                // Put the last poll in the list.
                lastStartedPoll = result.maxByOrNull { it.startedAt }
            }

            override fun onError(error: HMSException) {
            }

        })
    }

    fun getCurrentRoleChangeRequest() = pendingRoleChange

    fun getTrackForRolePendingChangeRequest(rolePreviewListener: RolePreviewListener) {
        val request = getCurrentRoleChangeRequest()
        request?.suggestedRole?.let { role ->
            hmsSDK.preview(role, object : RolePreviewListener {
                override fun onError(error: HMSException) {
                    rolePreviewListener.onError(error)
                }

                override fun onTracks(localTracks: Array<HMSTrack>) {
                    unMuteAllTracks(localTracks)
                    rolePreviewListener.onTracks(localTracks)
                }

                override fun onPermissionsRequested(permissions : List<String>) {
                    viewModelScope.launch {
                        _events.emit(Event.RequestPermission(permissions.toTypedArray()))
                    }
                }


            })
        }
    }

    private fun getMeetingTrack(trackId: String?): MeetingTrack? {
        return if (trackId == null)
            null
        else _tracks.find {
            it.video?.trackId == trackId
        }
    }

    private fun updateSelfHandRaised(hmsPeer: HMSLocalPeer) {
        val isSelfHandRaised = hmsPeer.isHandRaised
        _isHandRaised.postValue(isSelfHandRaised)
        _peerMetadataNameUpdate.postValue(Pair(hmsPeer, HMSPeerUpdate.METADATA_CHANGED))
    }

    private fun updateNameChange(hmsPeer: HMSLocalPeer) {
        _peerMetadataNameUpdate.postValue(Pair(hmsPeer, HMSPeerUpdate.NAME_CHANGED))
    }

    fun isServerRecordingEnabled(room: HMSRoom): Boolean {
        return room.serverRecordingState.state == HMSRecordingState.STARTED
    }

    fun isHlsRunning() = hmsSDK.getRoom()?.hlsStreamingState?.state == HMSStreamingState.STARTED
    fun isRTMPRunning() = hmsSDK.getRoom()?.rtmpHMSRtmpStreamingState?.state == HMSStreamingState.STARTED

    fun setStatetoOngoing() {
        state.postValue(MeetingState.Ongoing())
    }

    fun changeRoleAccept(onSuccess:() -> Unit = {}, onFailure:() -> Unit = {}) {
        pendingRoleChange?.let {
            hmsSDK.acceptChangeRole(it, object : HMSActionResultListener {
                override fun onSuccess() {
              //      toggleRaiseHand(false)
                    setStatetoOngoing()
                    recNum += 1
                    initPrebuiltChatMessageRecipient.postValue(Pair(prebuiltInfoContainer.defaultRecipientToMessage(), recNum))
                    updateThemeBasedOnCurrentRole(it.suggestedRole)
                    onSuccess.invoke()
                }

                override fun onError(error: HMSException) {
                    onFailure.invoke()
                    setStatetoOngoing()
                    Log.e(TAG, "Error while accepting change role request :: ${error.description}")
                    state.postValue(MeetingState.NonFatalFailure(error))
                }
            })

        }
    }

    private fun updateThemeBasedOnCurrentRole(suggestedRole: HMSRole) {
        hmsRoomLayout?.data?.findLast { it?.role == suggestedRole.name }?.themes?.getOrNull(0)?.palette?.let {
            setTheme(it)
        }
    }

    private fun isHlsPeer(localRole: HMSRole?) : Boolean{
          return  hmsRoomLayout?.data?.findLast { it?.role ==  localRole?.name }?.screens?.conferencing?.hlsLiveStreaming != null
    }

    //Checks if hand raised peer is eligble for viewer on stage
    private fun hasBringOnStageRolePermission(currentRole : HMSRole?, handRaisedPeerRole: HMSRole?) : Boolean{
        return  hmsRoomLayout?.data?.findLast { it?.role ==  currentRole?.name }?.screens?.conferencing?.default?.elements?.onStageExp?.offStageRoles?.contains(handRaisedPeerRole?.name)?:false
    }

    fun hasInsetEnabled(currentRole : HMSRole?) : Boolean = hmsRoomLayout?.data?.findLast { it?.role ==  currentRole?.name }?.screens?.conferencing?.default?.elements?.videoTileLayout?.grid?.enableLocalTileInset?:false

    fun isBRBEnabled() = hmsRoomLayout?.data?.findLast { it?.role ==  hmsSDK.getLocalPeer()?.hmsRole?.name }?.screens?.conferencing?.default?.elements?.brb != null
    fun isParticpantListEnabled() : Boolean = with(hmsRoomLayout?.data?.findLast { it?.role ==  hmsSDK.getLocalPeer()?.hmsRole?.name }?.screens?.conferencing) {
        this?.default?.elements?.participantList != null || this?.hlsLiveStreaming?.elements?.participantList != null
    }
    private fun getOnStageRole(currentRole : HMSRole?) = hmsRoomLayout?.data?.findLast { it?.role ==  currentRole?.name }?.screens?.conferencing?.default?.elements?.onStageExp?.onStageRole

    private fun switchToHlsView(streamUrl: String) {
        val currentMode = meetingViewMode.value
        if( currentMode is MeetingViewMode.HLS_VIEWER && currentMode.url == streamUrl) {
            // If there's nothing to change, don't restart hls fragment
        } else
        {
            meetingViewMode.postValue(MeetingViewMode.HLS_VIEWER(streamUrl))
        }
    }

    private fun exitHlsViewIfRequired(isHlsPeer: Boolean) {
        if (!isHlsPeer && meetingViewMode.value is MeetingViewMode.HLS_VIEWER) {
            meetingViewMode.postValue(MeetingViewMode.GRID)
        }
    }

    val showDvrControls = MutableLiveData<Boolean>()
    val showAudioIcon : MutableLiveData<Boolean> = MutableLiveData(false)
    val showHlsStreamYetToStartError = MutableLiveData<Boolean>(false)
    private fun switchToHlsViewIfRequired(role: HMSRole?, streamUrl: String?) {
        var started = false
        val isHlsPeer = isHlsPeer(role)
        showAudioIcon.postValue(!isHlsPeer)
        // If we don't check if the stream is started, it might try to open the hls view again when
        //  the stream was stopped. This happens when a running stream is stopped and buffers
        //  the stream.
        if (isHlsPeer && streamUrl != null) {
            started = true
            switchToHlsView(streamUrl)
        }

        if (isHlsPeer && streamUrl == null) {
            showHlsStreamYetToStartError.postValue(true)
        } else {
            showHlsStreamYetToStartError.postValue(false)
        }

        // Only send errors for those who are hls peers
        if (!started && isHlsPeer) {
            val reasons = mutableListOf<String>()
            if (streamUrl == null) {
                reasons.add("Stream url was null")
            }
//            HMSCoroutineScope.launch {
//                _events.emit(Event.HlsNotStarted("Can't switch to hls view. ${reasons.joinToString(",")}"))
//            }
        }
    }

    val isHlsKitUrl by lazy {
        settings.lastUsedMeetingUrl.contains("/streaming/")
    }

    fun switchToHlsViewIfRequired() {
        // get the hls URL from the Room, if it exists
        val hlsUrl = hmsSDK.getRoom()?.hlsStreamingState?.variants?.get(0)?.hlsStreamUrl
        showDvrControls.postValue(hmsSDK.getRoom()?.hlsStreamingState?.variants?.get(0)?.playlistType == HMSHLSPlaylistType.dvr)
        switchToHlsViewIfRequired(hmsSDK.getLocalPeer()?.hmsRole, hlsUrl)
    }

    fun flipCamera() {
        if (!settings.publishVideo) {
            error("Cannot switch camera when Video is disabled")
        }

        // NOTE: During audio-only calls, this switch-camera is ignored
        //  as no camera in use

        viewModelScope.launch {
            hmsSDK.getLocalPeer()?.videoTrack?.switchCamera(object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    viewModelScope.launch {
                        _events.emit(Event.CameraSwitchEvent("Error: $error"))
                    }
                }

                override fun onSuccess() {
                    viewModelScope.launch {
                        _events.emit(Event.CameraSwitchEvent("Success: Facing is now: ${hmsSDK.getLocalPeer()?.videoTrack?.settings?.cameraFacing}"))
                    }
                }

            })
        }
    }

    fun getAudioOutputRouteType() = hmsSDK.getAudioOutputRouteType()

    fun leaveMeeting(details: HMSRemovedFromRoom? = null) {
        hasValidToken = false
        state.postValue(MeetingState.Disconnecting("Disconnecting", "Leaving meeting"))
        // Don't call leave when being forced to leave
        if (details == null) {
            hmsSDK.leave()
        }
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
                track.source == "videoplaylist"
            ) {
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


            // Set the audio or video track of meetingTrack to null, so that UI updates accordingly
            when (track.type) {
                HMSTrackType.AUDIO -> {
                    meetingTrack?.audio = null
                }

                HMSTrackType.VIDEO -> {
                    meetingTrack?.video = null
                }
            }

            if (
            // Remove tile from view since both audio and video track are null for the peer
                (peer.audioTrack == null && peer.videoTrack == null) ||
                // Remove video screenshare/playlist tile from view
                ((track.source == HMSTrackSource.SCREEN || track.source == "videoplaylist")
                        && track.type == HMSTrackType.VIDEO)
            ) {
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

    fun isAllowedToCreatePolls() : Boolean {
        return hmsSDK.getLocalPeer()?.hmsRole?.permission?.pollWrite == true
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

    fun isAllowedToRtmpStream(): Boolean =
        hmsSDK.getLocalPeer()?.hmsRole?.permission?.rtmpStreaming == true

    fun isAllowedToBrowserRecord(): Boolean =
        hmsSDK.getLocalPeer()?.hmsRole?.permission?.browserRecording == true

    fun isAllowedToHlsStream(): Boolean =
        hmsSDK.getLocalPeer()?.hmsRole?.permission?.hlsStreaming == true

    fun isAllowedToEndRoom() :Boolean = hmsSDK.getLocalPeer()?.hmsRole?.permission?.endRoom == true

    fun isAllowedToShareScreen(): Boolean =
        hmsSDK.getLocalPeer()?.hmsRole?.publishParams?.allowed?.contains("screen") == true

    fun changeRole(remotePeerId: String, toRoleName: String, force: Boolean) {
        val hmsPeer = hmsSDK.getPeers().find { it.peerID == remotePeerId }
        val toRole = hmsSDK.getRoles().find { it.name == toRoleName }
        if (hmsPeer != null && toRole != null) {
            if (hmsPeer.hmsRole.name != toRole.name)
                hmsSDK.changeRoleOfPeer(hmsPeer, toRole, force, object : HMSActionResultListener {
                    override fun onSuccess() {
                        Log.i(TAG, "Successfully sent change role request for $hmsPeer")
                    }

                    override fun onError(error: HMSException) {
                        Log.e(
                            TAG,
                            "Error while sending change role request :: ${error.description}"
                        )
                        state.postValue(MeetingState.NonFatalFailure(error))
                    }
                })
            // Update the peer in participants
            peerLiveData.postValue(hmsPeer!!)
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

        val track = when (type) {
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
            hmsSDK.changeTrackState(
                mute,
                null,
                null,
                selectedRoles,
                object : HMSActionResultListener {
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
        rtmpInjectUrls: List<String> = emptyList(),
        inputWidthHeight: HMSRtmpVideoResolution? = null,
        runnable: Runnable? = null
    ) {
        // It's streaming if there are rtmp urls present.
        recordingState.postValue(HMSRecordingState.STARTING)
        Log.v(TAG, "Starting recording. url: $rtmpInjectUrls")
        hmsSDK.startRtmpOrRecording(
            HMSRecordingConfig(
                null,
                rtmpInjectUrls,
                isRecording,
                inputWidthHeight
            ), object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    recordingState.postValue(HMSRecordingState.FAILED)
                    Log.d(TAG, "RTMP recording error: $error")
                    // restore the current state
                    runnable?.run()
                    hmsNotificationEvent.postValue(
                        HMSNotification(
                            isError = true,
                            title = "Recording failed to start",
                            isDismissible = false,
                            icon = R.drawable.record_off,
                            actionButtonText = "Retry",
                            type = HMSNotificationType.RecordingFailedToStart
                        )
                    )
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

    fun startScreenshare(
        mediaProjectionPermissionResultData: Intent?,
        actionListener: HMSActionResultListener
    ) {
        // Without custom notification
//    hmsSDK.startScreenshare(actionListener ,mediaProjectionPermissionResultData)

        // With custom notification
        val notification = NotificationCompat.Builder(getApplication(), "ScreenCapture channel")
            .setContentText("Screenshare running for roomId: ${hmsRoom?.roomId}")
            .setSmallIcon(android.R.drawable.arrow_up_float)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Screenshare",
                HMSScreenCaptureService.getStopScreenSharePendingIntent(getApplication())
            )
            .build()

        hmsSDK.startScreenshare(actionListener, mediaProjectionPermissionResultData, notification)
    }

    fun isScreenShared() = hmsSDK.isScreenShared()

    fun stopScreenshare() {
        hmsSDK.stopScreenshare(object : HMSActionResultListener {
            override fun onError(error: HMSException) {
            }

            override fun onSuccess() {
                isScreenShare.postValue(false)
            }
        })
    }

    fun startAudioshare(
        mediaProjectionPermissionResultData: Intent?,
        audioMixingMode: AudioMixingMode,
        actionListener: HMSActionResultListener
    ) {
        // Without custom notification
//    hmsSDK.startScreenshare(actionListener ,mediaProjectionPermissionResultData)

        // With custom notification
        val notification = NotificationCompat.Builder(getApplication(), "ScreenCapture channel")
            .setContentText("Sharing Audio of device to roomId: ${hmsRoom?.roomId}")
            .setSmallIcon(android.R.drawable.arrow_up_float)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop sharing device audio",
                HMSScreenCaptureService.getStopScreenSharePendingIntent(getApplication())
            )
            .build()

        hmsSDK.startAudioshare(
            actionListener,
            mediaProjectionPermissionResultData,
            audioMixingMode,
            notification
        )
    }

    fun setAudioMixingMode(audioMixingMode: AudioMixingMode) {
        hmsSDK.setAudioMixingMode(audioMixingMode)
    }

    fun stopAudioshare(actionListener: HMSActionResultListener) {
        hmsSDK.stopAudioshare(actionListener)
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

        class HlsNotStarted(val reason: String) : Event()
        abstract class MessageEvent(open val message: String) : Event()
        data class RtmpEvent(val message: String) : Event()
        data class RecordEvent(val message: String) : Event()
        data class ServerRecordEvent(val message: String) : Event()
        data class HlsEvent(override val message: String) : MessageEvent(message)
        data class HlsRecordingEvent(override val message: String) : MessageEvent(message)
        data class CameraSwitchEvent(override val message: String) : MessageEvent(message)
        data class SessionMetadataEvent(override val message: String) : MessageEvent(message)
        data class PollStarted(val hmsPoll: HmsPoll) : Event()
        data class PollEnded(val hmsPoll : HmsPoll) : Event()
        data class PollVotesUpdated(val hmsPoll: HmsPoll) : Event()
        data class RequestPermission(val permissions : Array<String>) : Event()
    }

    private val _isHandRaised = MutableLiveData<Boolean>(false)
    val isHandRaised: LiveData<Boolean> = _isHandRaised


    fun toggleRaiseHand() {
        val localPeer = hmsSDK.getLocalPeer()
        localPeer?.let {
            if (it.isHandRaised) {
                lowerLocalPeerHand()
            } else {
                raiseLocalPeerHand()
            }
        }?: kotlin.run {
            Log.e(TAG, "Local Peer not present")
        }
    }

    private fun raiseLocalPeerHand() {
        hmsSDK.raiseLocalPeerHand(object : HMSActionResultListener{
            override fun onError(error: HMSException) {
                Log.e(TAG, "Error while raising hand $error")
            }

            override fun onSuccess() {
                Log.d(TAG, "Successfully raised hand")
            }
        })
    }

    fun lowerLocalPeerHand() {
        hmsSDK.lowerLocalPeerHand(object : HMSActionResultListener{
            override fun onError(error: HMSException) {
                Log.e(TAG, "Error while lowering hand $error")
            }

            override fun onSuccess() {
                Log.d(TAG, "Successfully lowered hand")
            }
        })
    }

    fun sendHlsMetadata(metaDataModel: HMSHLSTimedMetadata) {

        hmsSDK.setHlsSessionMetadata(arrayListOf(metaDataModel), object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                Log.d(TAG, "hls metadata sending failed")
            }

            override fun onSuccess() {
                Log.d(TAG, "hls metadata sent successfully")
            }
        })

    }

    fun isBRBOn(): Boolean {
        val localPeer = hmsSDK.getLocalPeer()?: return false
        val currentMetadata = CustomPeerMetadata.fromJson(localPeer.metadata)
        return currentMetadata?.isBRBOn?:false
    }

    fun toggleBRB() {
        val localPeer = hmsSDK.getLocalPeer()!!
        val currentMetadata = CustomPeerMetadata.fromJson(localPeer.metadata)
        val isBRB = currentMetadata!!.isBRBOn
        val newMetadataJson = currentMetadata.copy(isBRBOn = !isBRB).toJson()

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

    fun getStats(): Flow<Map<String, Any>> = statsFlow

    fun startHls(hlsUrl: String?, recordingConfig: HMSHlsRecordingConfig) {
        val meetingVariants = if (hlsUrl.isNullOrBlank()) {
            null
        } else null

        val config = HMSHLSConfig(
            meetingVariants,
            recordingConfig
        )

        hmsSDK.startHLSStreaming(config, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.Hls.HlsError(error))
                    streamingState.postValue(HMSStreamingState.FAILED)
                }
            }

            override fun onSuccess() {
                Log.d(TAG, "Hls streaming started successfully")
            }
        })
    }

    fun stopHls() {
        val config = HMSHLSConfig(emptyList())
        hmsSDK.stopHLSStreaming(config, object : HMSActionResultListener {
            override fun onSuccess() {
                Log.d(TAG, "Hls streaming stopped successfully")
            }

            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.Hls.HlsError(error))
                }

            }
        })
    }

    fun updateTrackStatus(status: String, isEnabled: Boolean) {
        _trackStatus.value = Pair(status, isEnabled)
    }

    var currentAudioMode = AudioManager.MODE_IN_COMMUNICATION

    fun toggleMediaMode() {
        currentAudioMode =
            if (currentAudioMode == AudioManager.MODE_IN_COMMUNICATION) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
        hmsSDK.setAudioMode(currentAudioMode)
    }

    fun getCurrentMediaModeCheckedState(): Boolean {
        return currentAudioMode != AudioManager.MODE_IN_COMMUNICATION
    }

    private val sessionMetadataUseCase: SessionMetadataUseCase = SessionMetadataUseCase()
    private lateinit var pinnedTrackUseCase: PinnedTrackUseCase
    private val blockUserUseCase : BlockUserUseCase = BlockUserUseCase()
    private val hideMessageUseCase : HideMessageUseCase = HideMessageUseCase()
    private val pauseChatUseCase : PauseChatUseCase = PauseChatUseCase()

    fun hideMessage(chatMessage: ChatMessage) {
        hideMessageUseCase.hideMessage(chatMessage, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.SessionMetadataEvent("Cannot hide too many messages."))
                }
            }

            override fun onSuccess() {
                Log.d(TAG, "Updating hide message list successful")
            }

        })
    }
    fun blockUser(chatMessage: ChatMessage) {
        blockUserUseCase.blockUser(chatMessage,object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.SessionMetadataEvent("Psst, too many peers blocked already."))
                }
            }

            override fun onSuccess() {
                Log.d(TAG, "Updating block successful")
            }

        })
        // For later
//        sessionMetadataUseCase.userBlocked(chatMessage)
    }
    val currentBlockList = blockUserUseCase.currentBlockList
    val messageIdsToHide = hideMessageUseCase.messageIdsToHide
    fun pinMessage(message : ChatMessage) {
        sessionMetadataUseCase.addToPinnedMessages(message, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.SessionMetadataEvent("Psst, you cannot pin large messages."))
                }
            }

            override fun onSuccess() {}
        })
    }

    fun unPinMessage(pinnedMessage: SessionMetadataUseCase.PinnedMessage) {
        sessionMetadataUseCase.removeFromPinnedMessages(pinnedMessage, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                viewModelScope.launch {
                    _events.emit(Event.SessionMetadataEvent("Session metadata removing pinned message ${error.message}"))
                }
            }

            override fun onSuccess() {}
        })
    }

    fun bulkRoleChange(toRole: HMSRole, rolesToChange: List<HMSRole>) {
        hmsSDK.changeRoleOfPeersWithRoles(rolesToChange, toRole, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                Log.d("bulkRoleChange", "There was an error $error")
            }

            override fun onSuccess() {
                Log.d("bulkRoleChange", "Successful")
            }

        })
    }

    val pinnedMessages: LiveData<Array<SessionMetadataUseCase.PinnedMessage>> = sessionMetadataUseCase.pinnedMessages

    override fun onCleared() {
        super.onCleared()
        sessionMetadataUseCase.close()
        blockUserUseCase.close()
        pauseChatUseCase.close()
        leaveMeeting()
    }

    fun isPrebuiltDebugMode(): Boolean {
        return isPrebuiltDebug
    }
    fun permissionGranted() = hmsSDK.setPermissionsAccepted()

    fun endPoll(hmsPoll: HmsPoll) = localHmsInteractivityCenter.stop(hmsPoll, object : HMSActionResultListener {
        override fun onError(error: HMSException) {
            Log.e("EndPoll","Error ending poll")
        }

        override fun onSuccess() {
            Log.d("EndPoll","Poll ended")
        }

    })

    fun fetchLeaderboard(pollId: String, completion: HmsTypedActionResultListener<PollLeaderboardResponse>) {
        localHmsInteractivityCenter.fetchLeaderboard(pollId, count = 200, completion = completion)
    }
    fun startPoll(currentList: List<QuestionUi>, pollCreationInfo: PollCreationInfo) {
        // To start a poll

        Log.d("Polls","$currentList")
        val hmsPollBuilder = HMSPollBuilder.Builder()
            .withTitle(pollCreationInfo.pollTitle)
            .withCategory(if (pollCreationInfo.isPoll) HmsPollCategory.POLL else HmsPollCategory.QUIZ)
            .withAnonymous(pollCreationInfo.hideVote)
            .withRolesThatCanVote(hmsSDK.getRoles().filter { it.name == "host" })
            .withRolesThatCanViewResponses(hmsSDK.getRoles().filter { it.name == "host" })

        currentList.forEach { questionUi ->
            Log.d("Polls","Processing $questionUi")

            when(questionUi) {
//                is QuestionUi.LongAnswer -> hmsPollBuilder.addLongAnswerQuestion(questionUi.text)
                is QuestionUi.MultiChoiceQuestion -> {
                    val multiChoice = HMSPollQuestionBuilder.Builder(HMSPollQuestionType.multiChoice)
                        .withTitle(questionUi.withTitle)
                    questionUi.options.forEachIndexed { index : Int, option : String ->
                        multiChoice.addQuizOption(option, questionUi.selections.contains(index))
                    }
                    hmsPollBuilder
                        .addQuestion(multiChoice.build())
                }
//                is QuestionUi.ShortAnswer -> hmsPollBuilder.addShortAnswerQuestion(questionUi.text)
                is QuestionUi.SingleChoiceQuestion -> {
                    val singleChoiceQuestionBuilder = HMSPollQuestionBuilder.Builder(HMSPollQuestionType.singleChoice)
                        .withTitle(questionUi.withTitle)
                    questionUi.options.forEachIndexed { index : Int, option : String ->
                        singleChoiceQuestionBuilder.addQuizOption(option, isCorrect = questionUi.selections.contains(index))
                    }
                    hmsPollBuilder
                        .addQuestion(singleChoiceQuestionBuilder.build())
                }
                is QuestionUi.QuestionCreator,
                is QuestionUi.LaunchButton,
                QuestionUi.AddAnotherItemView, -> { /*Nothing to do here*/}
            }
        }
        val pollBuilder = hmsPollBuilder.build()

        localHmsInteractivityCenter.quickStartPoll(pollBuilder, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                Log.d("Polls","Error $error")
            }

            override fun onSuccess() {
                Log.d("Polls","Success")
                // Now send a notification into the hls cue,
                //  it's ok if this fails since that just means there's no hls stream
                //  to send it into.
                // Might need to avoid sending it hls viewers though.
                val hlsPollEvent = HMSHLSTimedMetadata("$POLL_IDENTIFIER_FOR_HLS_CUE${pollBuilder.pollId}",1000)
                sendHlsMetadata(hlsPollEvent)
            }

        })
    }

    fun saveInfoText(question: HMSPollQuestion, answer : String, hmsPoll: HmsPoll) : Boolean {

        val valid = if(question.type == HMSPollQuestionType.shortAnswer &&
            answer.length > (question.answerShortMinLength ?: 0)
        ) {
            true
        }
        else question.type == HMSPollQuestionType.longAnswer &&
            (answer.length > (question.answerLongMinLength ?: 0) )

        if(valid) {
            val response = HMSPollResponseBuilder(hmsPoll, null)
                .addResponse(question, answer)
            localHmsInteractivityCenter.add(response, object : HmsTypedActionResultListener<PollAnswerResponse>{
                override fun onSuccess(result: PollAnswerResponse) {
                    Log.d("PollAnswer","Success $result")
                }

                override fun onError(error: HMSException) {
                    Log.d("PollAnswer","Error $error")
                }

            })
        }
        return valid
    }
    fun saveInfoSingleChoice(question : HMSPollQuestion, option: Int?, hmsPoll: HmsPoll, timeTakenMillis : Long) : Boolean {
        if(option == null) {
            return false
        }
        val answer = question.options?.get(option)
        if(answer != null) {
            val response = HMSPollResponseBuilder(hmsPoll, null)
                .addResponse(question, answer, timeTakenMillis)
            localHmsInteractivityCenter.add(response, object : HmsTypedActionResultListener<PollAnswerResponse>{
                override fun onSuccess(result: PollAnswerResponse) {
                    Log.d("PollAnswer","Success")
                }

                override fun onError(error: HMSException) {
                    Log.d("PollAnswer","Error $error")
                }

            })
        }
        return true
    }
    fun saveSkipped(question: HMSPollQuestion, hmsPoll: HmsPoll) {
//        val response = HMSPollResponseBuilder(hmsPoll, null)
//
//        localHmsInteractivityCenter.add()
    }
    fun saveInfoMultiChoice(question : HMSPollQuestion, options : List<Int>?, hmsPoll: HmsPoll, timeTakenMillis : Long) : Boolean {
        val valid = options != null
        val answer = question.options?.filterIndexed { index, hmsPollQuestionOption ->
            options?.contains(index) == true
        }
        if(valid && answer != null) {
            val response = HMSPollResponseBuilder(hmsPoll, null)
                .addResponse(question, answer, timeTakenMillis)
            localHmsInteractivityCenter.add(response, object : HmsTypedActionResultListener<PollAnswerResponse>{
                override fun onSuccess(result: PollAnswerResponse) {
                    Log.d("PollAnswer","Success $result")
                }

                override fun onError(error: HMSException) {
                    Log.d("PollAnswer","Error $error")
                }

            })
        }

        return valid
    }

    suspend fun getPollForPollId(pollId: String): HmsPoll? {
        val pollWithQuestions = CompletableDeferred<HmsPoll?>()
        val poll: HmsPoll? = localHmsInteractivityCenter.polls.find { it.pollId == pollId }

        if (poll == null)
            getAllPolls()

        localHmsInteractivityCenter.polls.find { it.pollId == pollId }
            ?.also { existingPoll ->
                if (existingPoll.questions == null || (existingPoll.questions?.isEmpty() == true)) {
                    localHmsInteractivityCenter.fetchPollQuestions(
                        existingPoll,
                        object : HmsTypedActionResultListener<List<HMSPollQuestion>> {
                            override fun onSuccess(result: List<HMSPollQuestion>) {
                                localHmsInteractivityCenter.polls.find { newPolls -> newPolls.pollId == pollId }
                                        ?.let { addResponses(it, pollWithQuestions)  }
                            }

                            override fun onError(error: HMSException) {
                                pollWithQuestions.completeExceptionally(error)
                            }

                        })
                } else if (existingPoll.questions?.flatMap { it.myResponses }?.isEmpty() != false) {
                    addResponses(existingPoll, pollWithQuestions)
                } else {
                    pollWithQuestions.complete(existingPoll)
                }
            }?:return null
        return try {
            pollWithQuestions.await()
        } catch (error: HMSException) {
            null
        }
    }

    private fun addResponses(requestedPoll: HmsPoll, pollWithQuestions: CompletableDeferred<HmsPoll?>, ) {
        localHmsInteractivityCenter.getResponses(requestedPoll, ownResponsesOnly = false, completion = object : HmsTypedActionResultListener<List<HMSPollQuestionResponse>> {
            override fun onSuccess(result: List<HMSPollQuestionResponse>) {
                pollWithQuestions.complete(localHmsInteractivityCenter.polls.find { existingPoll -> existingPoll.pollId == requestedPoll.pollId }!!)
            }

            override fun onError(error: HMSException) {
                pollWithQuestions.complete(requestedPoll)
            }

        })
    }

    fun hasPoll() : HmsPoll? = localHmsInteractivityCenter.polls.firstOrNull()

    fun hmsInteractivityCenterPolls() = localHmsInteractivityCenter.polls

    suspend fun getAllPolls() : List<HmsPoll>? {
        val getStartedPolls = CompletableDeferred<List<HmsPoll>>()
        localHmsInteractivityCenter.fetchPollList(HmsPollState.STARTED, object : HmsTypedActionResultListener<List<HmsPoll>>{
            override fun onSuccess(result: List<HmsPoll>) {
                getStartedPolls.complete(result)
            }

            override fun onError(error: HMSException) {
                getStartedPolls.completeExceptionally(error)
            }

        })
        val getEndedPolls = CompletableDeferred<List<HmsPoll>>()
        localHmsInteractivityCenter.fetchPollList(HmsPollState.STOPPED, object : HmsTypedActionResultListener<List<HmsPoll>>{
            override fun onSuccess(result: List<HmsPoll>) {
                getEndedPolls.complete(result)
            }

            override fun onError(error: HMSException) {
                getEndedPolls.completeExceptionally(error)
            }

        })

        return try {
            val polls = try {
                getStartedPolls.await()
            } catch (ex : HMSException) {
                emptyList()
            }
            polls.plus(try {
                getEndedPolls.await()
            } catch (ex : HMSException) {
                emptyList()
            })
        } catch (error : HMSException) {
            Log.d("AreTherePolls","$error")
            null
        }
    }
    fun lowerRemotePeerHand(hmsPeer: HMSPeer, hmsActionResultListener: HMSActionResultListener)
     = hmsSDK.lowerRemotePeerHand(hmsPeer, hmsActionResultListener)

    fun requestBringOnStage(handRaisePeer: HMSPeer, onStageRole: String) {
        val force = prebuiltInfoContainer.shouldForceRoleChange()
        changeRole(handRaisePeer.peerID, onStageRole, force)

        if(force) {
            hmsSDK.lowerRemotePeerHand(handRaisePeer, object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    Log.d(TAG,"Failed to lower peer's hand $error")
                }

                override fun onSuccess() {
                    Log.d(TAG,"Lowered peer's hand since the role was force changed")
                }

            })
        }
    }

    fun triggerErrorNotification(message: String, isDismissible: Boolean = true, type: HMSNotificationType = HMSNotificationType.Error, actionButtonText:String ="") {
        hmsNotificationEvent.postValue(
            HMSNotification(
                title = "Error ${message}",
                isError = true,
                isDismissible = isDismissible,
                icon = R.drawable.ic_alert_triangle,
                type = type,
                actionButtonText = actionButtonText
            )
        )
    }

    fun triggerScreenShareBottomSheet(video: HMSVideoTrack?) {
        video?.trackId?.let {
            hmsScreenShareBottomSheetEvent.postValue(it)
        }
    }

    fun triggerScreenShareNotification(showScreenshare: Boolean) {
        if (showScreenshare) {
            hmsNotificationEvent.postValue(
                HMSNotification(
                    title = "You are sharing your screen",
                    isError = true,
                    isDismissible = false,
                    icon = R.drawable.share_screen_on,
                    type = HMSNotificationType.ScreenShare,
                    actionButtonText = "Stop"
                )
            )
        } else {
            hmsRemoveNotificationEvent.postValue(HMSNotificationType.ScreenShare)
        }

    }

    fun getPeerlistIterator(roleName: String): PeerListIterator {
        val options = PeerListIteratorOptions(byRoleName = roleName)
        return hmsSDK.getPeerListIterator(options)
    }

    fun getFullPeerlistIterator(): PeerListIterator {
        return hmsSDK.getPeerListIterator()
    }

    fun showPollOnUi(): Boolean {
        return hmsSDK.getLocalPeer()?.hmsRole?.permission?.pollRead == true || hmsSDK.getLocalPeer()?.hmsRole?.permission?.pollWrite == true
    }

    fun isAllowedToBlockFromChat(): Boolean = prebuiltInfoContainer.isAllowedToBlockUserFromChat()

    fun isAllowedToPinMessages(): Boolean = prebuiltInfoContainer.isAllowedToPinMessages()

    fun availableRecipientsForChat()  = prebuiltInfoContainer.allowedToMessageWhatParticipants()
    fun isAllowedToPauseChat() : Boolean = prebuiltInfoContainer.isAllowedToPauseChat()

    fun isAllowedToHideMessages() : Boolean = prebuiltInfoContainer.isAllowedToHideMessages()

    fun togglePauseChat() {
        val newState = chatPauseState.value!!
        val localPeer = hmsSDK.getLocalPeer()
        val updatedBy = with(localPeer) {
            if(this == null) ChatPauseState.UpdatedBy() else
            ChatPauseState.UpdatedBy(
                peerID ?: "",
                customerUserID ?: "",
                name ?: "Participant"
            )
        }
        newState.copy(enabled = !newState.enabled,
            updatedBy = updatedBy
        )
        pauseChatUseCase.changeChatState(newState)
    }

    val chatPauseState = pauseChatUseCase.currentChatPauseState
    fun defaultRecipientToMessage() = prebuiltInfoContainer.defaultRecipientToMessage()

    fun chatTitle() = prebuiltInfoContainer.getChatTitle()

    fun shouldSkipPreview() = prebuiltInfoContainer.shouldSkipPreview()

    private var playerStarted = false
    fun hlsPlayerBeganToPlay() {
        val lp = lastStartedPoll
        if(lp == null) {
            playerStarted = true
            return
        }

        val currentUnixTimestampInSeconds = (System.currentTimeMillis()/1000L)
        val isPollLaunchedGreaterThan20SecondsAgo = currentUnixTimestampInSeconds - lp.startedAt > 20
        if(!playerStarted && isPollLaunchedGreaterThan20SecondsAgo) {
            viewModelScope.launch {
                triggerPollsNotification(lp)
            }
        }
        playerStarted = true
    }

    fun disableNameEdit() = prebuiltOptions?.userName != null

    val countDownTimerStartedAt = MutableLiveData<Long?>(null)
    fun setCountDownTimerStartedAt(startedAt: Long?) {
        countDownTimerStartedAt.postValue(startedAt)
    }

    fun updateAudioDeviceChange(p0: HMSAudioManager.AudioDevice) {
        audioDeviceChange.postValue(p0)
    }

    private val questionTimingUseCase = QuizQuestionTimingUseCase()
    val setQuestionStartTime = questionTimingUseCase::setQuestionStartTime
    val getQuestionStartTime = questionTimingUseCase::getQuestionStartTime

    fun getLogo() = getHmsRoomLayout()?.data?.getOrNull(0)?.logo?.url

//    var hmsPlayer : HmsHlsPlayer? = null
//    fun setHLSPlayer(player: HmsHlsPlayer) {
//        hmsPlayer = player
//    }
//
//    fun getHLSPLayer() = hmsPlayer

    fun getLiveStreamingHeaderTitle(): String?{
        return prebuiltInfoContainer.getLiveStreamingHeaderTitle()
    }
    //fun getHeader() = getHmsRoomLayout()?.data?.getOrNull(0)?.screens?.conferencing?.hlsLiveStreaming?.elements?.participantList
    fun toggleNoiseCancellation() : Boolean {
        hmsSDK.setNoiseCancellationEnabled(!hmsSDK.getNoiseCancellationEnabled())
        return hmsSDK.getNoiseCancellationEnabled()
    }

    fun isNoiseCancellationEnabled() : Boolean = hmsSDK.getNoiseCancellationEnabled()
    // Show the NC button if it's a webrtc peer with noise cancellation available
    fun displayNoiseCancellationButton() : Boolean = hmsSDK.isNoiseCancellationAvailable() == AvailabilityStatus.Available && ( hmsSDK.getLocalPeer()?.let { !isHlsPeer(it.hmsRole) } ?: false )
}

