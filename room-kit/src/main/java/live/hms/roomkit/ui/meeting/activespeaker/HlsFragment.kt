package live.hms.roomkit.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.TimeBar
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.otaliastudios.zoom.ZoomSurfaceView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.hms.hls_player.*
import live.hms.roomkit.databinding.HlsFragmentLayoutBinding
import live.hms.roomkit.databinding.LayoutChatMergeBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.roomkit.ui.meeting.MeetingFragment
import live.hms.roomkit.ui.meeting.MeetingFragmentDirections
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.PauseChatUIUseCase
import live.hms.roomkit.ui.meeting.SessionOptionBottomSheet
import live.hms.roomkit.ui.meeting.bottomsheets.LeaveCallBottomSheet
import live.hms.roomkit.ui.meeting.bottomsheets.StreamEnded
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.meeting.chat.combined.ChatRbacRecipientHandling
import live.hms.roomkit.ui.meeting.chat.combined.LaunchMessageOptionsDialog
import live.hms.roomkit.ui.meeting.chat.combined.PinnedMessageUiUseCase
import live.hms.roomkit.ui.meeting.chat.rbac.RoleBasedChatBottomSheet
import live.hms.roomkit.ui.meeting.compose.Variables
import live.hms.roomkit.ui.meeting.compose.Variables.Companion.PrimaryDefault
import live.hms.roomkit.ui.meeting.compose.Variables.Companion.Spacing0
import live.hms.roomkit.ui.meeting.compose.Variables.Companion.Spacing1
import live.hms.roomkit.ui.meeting.compose.Variables.Companion.Spacing2
import live.hms.roomkit.ui.polls.leaderboard.millisToText
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.getStringForTime
import live.hms.roomkit.util.viewLifecycle
import live.hms.roomkit.util.visibility
import live.hms.stats.PlayerStatsListener
import live.hms.stats.Utils
import live.hms.stats.model.PlayerStatsModel
import live.hms.video.error.HMSException
import live.hms.video.sdk.models.enums.HMSRecordingState
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds


/**
 * If the stream is this many seconds behind live
 *  show the live buttons.
 */
private const val MILLI_SECONDS_FROM_LIVE = 10_000

@UnstableApi class HlsFragment : Fragment() {
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val displayHlsCuesUseCase by lazy {
        DisplayHlsCuesUseCase({ text -> binding.hlsCues.text = text }) { pollId ->
            lifecycleScope.launch {
                val hmsPoll = meetingViewModel.getPollForPollId(pollId)
                if (hmsPoll != null) meetingViewModel.triggerPollsNotification(hmsPoll)
            }
        }
    }
    private val hlsViewModel: HlsViewModel by activityViewModels {
        HlsViewModelFactory(requireActivity().application,args.hlsStreamUrl, meetingViewModel.hmsSDK,
            meetingViewModel::hlsPlayerBeganToPlay
        ) { displayHlsCuesUseCase }
    }
    private val player by lazy { hlsViewModel.player }
    private val chatViewModel: ChatViewModel by activityViewModels()
    private val pinnedMessageUiUseCase = PinnedMessageUiUseCase()
    private val launchMessageOptionsDialog = LaunchMessageOptionsDialog()

    private val builder = StringBuilder()
    private val formatter = Formatter(builder, Locale.getDefault())

    private val chatAdapter by lazy {
        ChatAdapter({ message ->
            launchMessageOptionsDialog.launch(
                meetingViewModel, childFragmentManager, message
            )
        },
            {},
            { message -> MessageOptionsBottomSheet.showMessageOptions(meetingViewModel, message) })
    }

    val TAG = "HlsFragment"
    var isStatsDisplayActive: Boolean = false

    //    private val player by lazy{ HmsHlsPlayer(requireContext(), meetingViewModel.hmsSDK) }


    private lateinit var composeView: ComposeView

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)
        composeView = binding.composeView
        return binding.root
    }

    private fun goLive(player: HmsHlsPlayer) {
        hlsViewModel.isPlaying.postValue(true)
        with(player.getNativePlayer()){
            play()
            seekToDefaultPosition()
        }
    }
    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hlsViewModel.streamEndedEvent.observe(viewLifecycleOwner) {
            StreamEnded.launch(parentFragmentManager)
        }
        binding.applyTheme()
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var controlsVisible by remember { mutableStateOf(false) }
                var closedCaptionsEnabled by remember { mutableStateOf(true) }
                val isPlaying by hlsViewModel.isPlaying.observeAsState()
                val isLive by hlsViewModel.isLive.observeAsState(false)
                val behindLiveByState by hlsViewModel.behindLiveByLiveData.observeAsState("00:00")
                val viewers by meetingViewModel.peerCount.observeAsState()
                val elapsedTime by meetingViewModel.countDownTimerStartedAt.observeAsState()
                var ticks by remember { mutableLongStateOf(0) }
                val recordingState by meetingViewModel.recordingState.observeAsState()

                LaunchedEffect(elapsedTime) {
                    elapsedTime?.let {
                        ticks = System.currentTimeMillis().minus(it)
                        while (true) {
                            delay(1.seconds)
                            ticks += 1000
                        }
                    }
                }


                val progressBarVisibility by hlsViewModel.progressBarVisible.observeAsState()
                val viewMode by meetingViewModel.state.observeAsState()

                // Don't show whole view loading during the time it's disconnected or reconnecting.
                if (progressBarVisibility == true || viewMode !is MeetingState.Ongoing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        color = PrimaryDefault
                    )
                } else {
                    val isChatEnabled by rememberSaveable { mutableStateOf(meetingViewModel.prebuiltInfoContainer.isChatEnabled()) }
                    var chatOpen by remember { mutableStateOf(isChatEnabled)}
                    val isHandRaised by meetingViewModel.isHandRaised.observeAsState(false)
                    val showDvrControls by meetingViewModel.showDvrControls.observeAsState(false)

                    fun openSessionOptions() {
                        SessionOptionBottomSheet(
                            onScreenShareClicked = {  },
                            onBRBClicked = {  },
                            onPeerListClicked = {
                                meetingViewModel.launchParticipantsFromHls.postValue(Unit)
                            },
                            onRaiseHandClicked = { meetingViewModel.toggleRaiseHand()},
                            onNameChange = {  },
                            showPolls = { openPolls() },
                            onRecordingClicked = {},
                            disableHandRaiseDisplay = true
                        ).show(
                            childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
                        )
                    }

                    OrientationSwapper({ isLandScape ->
                        Row {

                            HlsComposable(
                                isChatEnabled = isChatEnabled,
                                hlsViewModel = hlsViewModel,
                                controlsVisible = controlsVisible,
                                videoTapped = { controlsVisible = !controlsVisible },
                                context = context,
                                player = player,
                                settingsButtonTapped = { showTrackSelection(player) },
                                maximizeClicked = {chatOpen = !chatOpen },
                                closedCaptionsButton = {ClosedCaptionsButton({ closedCaptionsEnabled = !closedCaptionsEnabled}, closedCaptionsEnabled)},
                                pauseButton = {
                                    PlayPauseButton({if(player.getNativePlayer().isPlaying) {
                                    player.getNativePlayer().pause()
                                    hlsViewModel.isLive.postValue(false)
                                }
                                else {
                                    player.getNativePlayer().play()
                                }
                                hlsViewModel.isPlaying.postValue(isPlaying?.not())
                                                               }, isPlaying)},
                                hlsChatIcon = {if(!chatOpen) HlsChatIcon(isChatEnabled){chatOpen = !chatOpen}},
                                chatOpen = chatOpen,
                                isLandscape = isLandScape,
                                isLive = isLive,
                                behindBy = behindLiveByState,
                                goLiveClicked = {goLive(player)},
                                onCloseButtonClicked = { LeaveCallBottomSheet().show(parentFragmentManager, null)},
                                closedCaptionsEnabled = closedCaptionsEnabled,
                                isHandRaised = isHandRaised,
                                toggleHandRaise = meetingViewModel::toggleRaiseHand,
                                sessionOptionsButtonTapped = ::openSessionOptions,
                                showDvrControls = showDvrControls,
                                forwardButton = { ForwardButton(isLive) {
                                    player.seekForward(10, TimeUnit.SECONDS)
                                } },
                                rewindButton = {RewindButton {
                                    player.seekBackward(10, TimeUnit.SECONDS)
                                }},
                            )
                            Column {
                                if(chatOpen) {
                                    ChatUI(
                                        childFragmentManager,
                                        chatViewModel,
                                        meetingViewModel,
                                        pinnedMessageUiUseCase,
                                        chatAdapter,
                                        ::openPolls
                                    )
                                }
                            }
                        }
                    }, { isLandscape ->
                        Column {
                            HlsComposable(
                                isChatEnabled = isChatEnabled,
                                hlsViewModel = hlsViewModel,
                                controlsVisible = controlsVisible,
                                videoTapped = { controlsVisible = !controlsVisible },
                                context = context,
                                player = player,
                                settingsButtonTapped = { showTrackSelection(player) },
                                maximizeClicked = {chatOpen = !chatOpen },
                                closedCaptionsButton = {ClosedCaptionsButton({ closedCaptionsEnabled = !closedCaptionsEnabled}, closedCaptionsEnabled)},
                                pauseButton = {
                                    PlayPauseButton({if(player.getNativePlayer().isPlaying) {
                                        player.getNativePlayer().pause()
                                        hlsViewModel.isLive.postValue(false)
                                    }
                                    else {
                                        player.getNativePlayer().play()
                                    }
                                        hlsViewModel.isPlaying.postValue(isPlaying?.not())
                                    }, isPlaying)},
                                hlsChatIcon = {if(!chatOpen) HlsChatIcon(isChatEnabled){chatOpen = !chatOpen}},
                                chatOpen = chatOpen,
                                isLandscape = isLandscape,
                                isLive = isLive,
                                behindBy = behindLiveByState,
                                goLiveClicked = {goLive(player)},
                                onCloseButtonClicked = {LeaveCallBottomSheet().show(parentFragmentManager, null)},
                                closedCaptionsEnabled = closedCaptionsEnabled,
                                isHandRaised = isHandRaised,
                                toggleHandRaise = meetingViewModel::toggleRaiseHand,
                                sessionOptionsButtonTapped = ::openSessionOptions,
                                showDvrControls = showDvrControls,
                                forwardButton = { ForwardButton(isLive) {
                                    player.seekForward(10, TimeUnit.SECONDS)
                                    player.getNativePlayer().play()
                                }},
                                rewindButton = { RewindButton {
                                    player.seekBackward(10, TimeUnit.SECONDS)
                                    player.getNativePlayer().play()
                                } },
                            )
                            if(chatOpen) {
                                ChatHeader(
                                    meetingViewModel.getLiveStreamingHeaderTitle(), meetingViewModel.getLogo(),
                                    viewers ?:0,
                                    ticks,
                                    recordingState
                                )
                                ChatUI(
                                    childFragmentManager,
                                    chatViewModel,
                                    meetingViewModel,
                                    pinnedMessageUiUseCase,
                                    chatAdapter,
                                    ::openPolls
                                )
                            }
                        }
                    })

                }

                PauseWhenLeaving(player)
                RemoveStatsWhenPaused(::setPlayerStatsListener, player)

            }
        }
        statsObservers()

    }

    private fun openPolls() {
        findNavController().navigate(MeetingFragmentDirections.actionMeetingFragmentToPollsCreationFragment())
    }

    private fun statsObservers() {
        meetingViewModel.statsToggleData.observe(viewLifecycleOwner) {
            isStatsDisplayActive = it
            setStatsVisibility(it)
        }
    }

    private fun statsToString(playerStats: PlayerStatsModel): String {
        return "bitrate : ${
            Utils.humanReadableByteCount(
                playerStats.videoInfo.averageBitrate.toLong(), true, true
            )
        }/s \n" + "bufferedDuration  : ${playerStats.bufferedDuration.absoluteValue / 1000} s \n" + "video width : ${playerStats.videoInfo.videoWidth} px \n" + "video height : ${playerStats.videoInfo.videoHeight} px \n" + "frame rate : ${playerStats.videoInfo.frameRate} fps \n" + "dropped frames : ${playerStats.frameInfo.droppedFrameCount} \n" + "distance from live edge : ${
            playerStats.distanceFromLive.div(
                1000
            )
        } s"
    }
    private fun setStatsVisibility(enable: Boolean) {
        if (isStatsDisplayActive && enable) {
            binding.statsViewParent.visibility = View.VISIBLE
        } else {
            binding.statsViewParent.visibility = View.GONE
        }
    }

    private fun setPlayerStatsListener(enable: Boolean, player: HmsHlsPlayer) {
        Log.d("SetPlayerStats", "display: ${isStatsDisplayActive} && enable: ${enable}")

        if (enable) {
            player.setStatsMonitor(object : PlayerStatsListener {
                override fun onError(error: HMSException) {
                    Log.d(TAG, "Error $error")
                }

                @SuppressLint("SetTextI18n")
                override fun onEventUpdate(playerStatsModel: PlayerStatsModel) {
                    updateLiveButtonVisibility(playerStatsModel)
                    if (isStatsDisplayActive) {
                        updateStatsView(playerStatsModel)
                    }
                }
            })
        } else {
            player.setStatsMonitor(null)
        }
    }

    fun updateStatsView(playerStats: PlayerStatsModel) {
        binding.bandwidthEstimateTv.text = "${
            Utils.humanReadableByteCount(
                playerStats.bandwidth.bandWidthEstimate, si = true, isBits = true
            )
        }/s"

        binding.networkActivityTv.text = "${
            Utils.humanReadableByteCount(
                playerStats.bandwidth.totalBytesLoaded, si = true, isBits = true
            )
        }"

        binding.statsView.text = statsToString(playerStats)
    }

    fun updateLiveButtonVisibility(playerStats: PlayerStatsModel) {
        // It's live if the distance from the live edge is less than 10 seconds.
        val isLive = playerStats.distanceFromLive < MILLI_SECONDS_FROM_LIVE
        hlsViewModel.isLive.postValue(isLive)
        hlsViewModel.behindLiveByLiveData.postValue(getStringForTime(builder, formatter, playerStats.distanceFromLive*-1) )
        // Show the button to go to live if it's not live.
    }

    private fun showTrackSelection(player: HmsHlsPlayer) {
        val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(player)
        trackSelectionBottomSheet.show(
            requireActivity().supportFragmentManager, "trackSelectionBottomSheet"
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChatHeader(headingText: String?, logoUrl: String?, viewers: Int, startedMillis: Long,
               recordingState : HMSRecordingState?
) {
    fun getViewersDisplayNum(viewers: Int): String = if (viewers < 1000) {
        "$viewers"
    } else "${viewers / 1000f}K"

    fun getTimeDisplayNum(startedMillis: Long): String = millisToText(startedMillis, false, "s")

    Column {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(Spacing2),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {

        GlideImage(
            model = logoUrl,
            loading = if (LocalInspectionMode.current) placeholder(R.drawable.exo_edit_mode_logo) else null,
            contentDescription = "Logo"
        )
        Column {
            headingText?.let {
                Text(
                    it, style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                        fontWeight = FontWeight(600),
                        color = Variables.OnSecondaryHigh,
                        letterSpacing = 0.1.sp,
                    )
                )
            }

            Text(
                "${getViewersDisplayNum(viewers)} watching · Started ${
                    getTimeDisplayNum(
                        startedMillis
                    )
                } ago${if (recordingState == HMSRecordingState.STARTED) " · Recording" else "" }", style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                    fontWeight = FontWeight(400),
                    color = Variables.OnSurfaceMedium,
                    letterSpacing = 0.4.sp,
                )
            )

        }
    }
    Divider (
        color = Variables.BorderBright,
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
    }
}

@Preview
@Composable
fun ChatHeaderPreview() {
    ChatHeader(
        headingText = "Tech talks",
        "https://storage.googleapis.com/100ms-cms-prod/cms/100ms_18a29f69f2/100ms_18a29f69f2.png",
        1000,
        30 * 60 * 1000,
        HMSRecordingState.STARTING
    )
}

@Composable
fun Chat(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing1),
        horizontalAlignment = Alignment.Start,
    ) {
        items(messages) {
            ChatMessage(
                name = it.senderName, message = it.message
            )
        }
    }
}

@Composable
fun ChatMessage(name: String, message: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            name, style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                fontWeight = FontWeight(600),
                color = Variables.OnSurfaceHigh,
                letterSpacing = 0.1.sp,
            )
        )
        Text(
            message, style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                fontWeight = FontWeight(400),
                color = Variables.OnSurfaceHigh,
                letterSpacing = 0.25.sp,
            )
        )
    }
}

@Preview
@Composable
fun ChatPreview() {
    val m1 = ChatMessage(
        "Chris Pine",
        "Chris",
        null,
        "Hi this is chris",
        true,
        false,
        false,
        null,
        null,
        null,
        null,
        null
    )
    val messages = listOf(
        m1, m1
    )
    Chat(messages)
}

@Composable
fun HlsBottomBar(isChatEnabled : Boolean, isLive : Boolean?, behindBy: String, isMaximized: Boolean, maximizeClicked : () -> Unit, goLiveClicked: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically){
        GoLiveText(isLive ?: false, behindBy, goLiveClicked)
        Spacer(Modifier.weight(1f))
        if(isChatEnabled) {
            MaximizeButton(maximizeClicked, isMaximized)
        }
    }
}
@Preview
@Composable
fun BottomBarPreview() {
    HlsBottomBar(true,false,"-01:20",false,{}){}
}
@Composable
fun GoLiveText(isLive : Boolean, behindBy: String, goLiveClicked : () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {

        Image(
            painter = painterResource(id =
            if(isLive) live.hms.roomkit.R.drawable.hls_live_dot else  live.hms.roomkit.R.drawable.hls_go_live_dot),
            contentDescription = "Gray",
            modifier = Modifier
                // Margin right
                .padding(end = Spacing1)
        )
        Text(
            text = if(isLive) "LIVE" else "GO LIVE",
            modifier = Modifier.clickable { goLiveClicked() },
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                fontWeight = FontWeight(600),
                color = if(isLive) Variables.OnSurfaceHigh else Variables.OnSurfaceMedium,
                letterSpacing = 0.5.sp,
            )
        )
        AnimatedVisibility(visible = !isLive) {
            Text(
                text = behindBy,
                modifier = Modifier
                    .clickable { goLiveClicked() }
                    .padding(start = Spacing1),
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                    fontWeight = FontWeight(600),
                    color = if(isLive) Variables.OnSurfaceHigh else Variables.OnSurfaceMedium,
                    letterSpacing = 0.5.sp,
                )
            )
        }
    }
}
@Composable
fun MaximizeButton(
    onClickAction: () -> Unit,
    isMaximized : Boolean
) {
    Image(painter = painterResource(id = if(isMaximized) live.hms.roomkit.R.drawable.hls_minimize else live.hms.roomkit.R.drawable.hls_maximize),
        contentScale = ContentScale.None,
        contentDescription = "Maximize Video",
        modifier = Modifier
            .clickable { onClickAction() }
            .padding(1.dp)
            .size(32.dp))
}
@Composable
fun SettingsButton(
    onClickAction: () -> Unit
) {
    Image(painter = painterResource(id = live.hms.roomkit.R.drawable.settings),
        contentDescription = "Layer Select",
        modifier = Modifier
            .clickable { onClickAction() }
            .padding(1.dp)
            .size(32.dp))
}
//
//val configuration = LocalConfiguration.current
//when (configuration.orientation) {
//    Configuration.ORIENTATION_LANDSCAPE -> {
//        Text("Landscape")
//    }
//    else -> {
//        Text("Portrait")
//    }
//}

@OptIn(ExperimentalComposeUiApi::class)
@UnstableApi
@Composable
fun HlsComposable(
    hlsViewModel: HlsViewModel,
    controlsVisible: Boolean,
    videoTapped: () -> Unit,
    context: Context,
    player: HmsHlsPlayer,
    settingsButtonTapped: () -> Unit,
    maximizeClicked: () -> Unit,
    pauseButton : @Composable () -> Unit,
    closedCaptionsButton : @Composable () -> Unit,
    hlsChatIcon : @Composable () -> Unit,
    chatOpen : Boolean,
    isLandscape : Boolean,
    isLive : Boolean?,
    behindBy : String,
    isChatEnabled : Boolean,
    goLiveClicked : () -> Unit,
    onCloseButtonClicked: () -> Unit,
    closedCaptionsEnabled : Boolean,
    isHandRaised : Boolean,
    toggleHandRaise : () -> Unit,
    sessionOptionsButtonTapped : () -> Unit,
    showDvrControls : Boolean,
    forwardButton: @Composable () -> Unit,
    rewindButton : @Composable () -> Unit,
) {

    lateinit var scaleGestureListener : ScaleGestureDetector
    // Keeping it one box so rows and columns don't change the layout
    Box {

        val hlsModifier = if(chatOpen && !isLandscape) {
            //hlsViewModel.resizeMode.postValue(RESIZE_MODE_FIT)
            Modifier
                .aspectRatio(ratio = 16f / 9)
//                .pointerInput(Unit) {
//                    detectTapGestures(onTap = {
//                        videoTapped()
//                    })
//                }
                .fillMaxWidth()
        } else if(chatOpen && isLandscape) {
            //hlsViewModel.resizeMode.postValue(RESIZE_MODE_FIT)
            Modifier
//                .pointerInput(Unit) {
//                    detectTapGestures(onTap = {
//                        videoTapped()
//                    })
//                }
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
        }
        else {
            Modifier
                // add transformable to listen to multitouch transformation events
                // after offset
                .fillMaxSize()
//                .pointerInteropFilter { event ->
//                    scaleGestureListener.onTouchEvent(event)
//                }
//                .pointerInput(Unit) {
//                    detectTapGestures(onTap = {
//                        videoTapped()
//                    })
//                }
        }

        AndroidView(modifier = hlsModifier, factory = {
            val tapGestureHandler = GestureDetector(it, ShortTapListener(videoTapped))
            ZoomSurfaceView(it).apply {
                setOnTouchListener { _, motionEvent ->
                    tapGestureHandler.onTouchEvent(motionEvent)
                }
                addCallback(object : ZoomSurfaceView.Callback {
                    override fun onZoomSurfaceCreated(view: ZoomSurfaceView) {
                        player.getNativePlayer().setVideoSurface(view.surface)
                    }
                    override fun onZoomSurfaceDestroyed(view: ZoomSurfaceView) {
                        player.getNativePlayer().setVideoSurface(null)
                    }
                })

                player.getNativePlayer().addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                         setContentSize(videoSize.width.toFloat(), videoSize.height.toFloat())
                    }
                })
            }


        }, onRelease = {
            player.getNativePlayer().setVideoSurface(null)
        })


        if (closedCaptionsEnabled) {
            BoxWithConstraints(
                modifier = hlsModifier.pointerInteropFilter {
                    // allow touches to passthrough
                    false
                                                            },
                contentAlignment = Alignment.BottomCenter,
            ) {

                val subtitles by hlsViewModel.currentSubtitles.observeAsState()
                if(!subtitles.isNullOrEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = Spacing0)) {
                        Surface(color = Color.DarkGray) {
                            Text(
                                text = subtitles ?: "",
                                modifier = Modifier.padding(Spacing1),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_bold)),
                                    fontWeight = FontWeight(600),
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                )
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            if(showDvrControls) {
                DvrControls(hlsModifier, player, forwardButton = forwardButton,
                    rewindButton = rewindButton,
                    playPauseButton = pauseButton)
            }
            // Draw the items in a grid with the same size as
            // the hls video by applying hls video size with BoxWithConstraints.
            BoxWithConstraints(modifier = hlsModifier,
                contentAlignment = Center) {

                // There's one column, with two rows.
                // A spacer puts a gap between items on any one row.
                Column(Modifier.padding(Spacing1)) {
                    // Top Row
                    Row {

                        Spacer(modifier = Modifier.weight(1f))

                        hlsChatIcon()

                        Spacer(modifier = Modifier.padding(start = Spacing2))

                        closedCaptionsButton()

                        Spacer(modifier = Modifier.padding(start = Spacing2))

                        SettingsButton(settingsButtonTapped)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Bottom Row
                    HlsBottomBar(
                        isChatEnabled = isChatEnabled,
                        isLive = isLive,
                        behindBy = behindBy,
                        isMaximized = !chatOpen,
                        maximizeClicked = maximizeClicked,
                        goLiveClicked = goLiveClicked
                    )
                }
                // We don't know about DVR yet so pause might not be possible.
//                pauseButton()
            }
        }

        AnimatedVisibility(
            visible = !(isLandscape && !chatOpen && !controlsVisible),
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            CloseButton(onCloseButtonClicked)
        }
        if (!isChatEnabled) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(vertical = Spacing0),
                contentAlignment = Alignment.BottomCenter) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // chat button
                // settings button
                HandRaiseButton(isHandRaised, toggleHandRaise)
                SessionOptionsButton(sessionOptionsButtonTapped)
            }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun DvrControls(modifier: Modifier, player: HmsHlsPlayer, playPauseButton : @Composable () -> Unit,
                rewindButton : @Composable () -> Unit,
                forwardButton : @Composable () -> Unit ) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)){
            rewindButton()
            playPauseButton()
            forwardButton()
        }
        Column {
            Spacer(Modifier.weight(1f))
            AndroidView(modifier = Modifier.wrapContentSize().padding(top = Spacing1), factory = {
                (LayoutInflater.from(it)
                    .inflate(live.hms.roomkit.R.layout.player_controls, null) as PlayerControlView)
                    .apply {
                        setPlayer(player.getNativePlayer())
                        this.findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_center_controls).visibility =
                            View.GONE
                        this.findViewById<View>(androidx.media3.ui.R.id.exo_controls_background).visibility =
                            View.GONE
                        this.findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar).visibility =
                            View.GONE
                    }
            }, update = {
            })
        }
    }
}

@Composable
fun SessionOptionsButton(onClick: () -> Unit) {
    Image(painter = painterResource(id = live.hms.roomkit.R.drawable.compose_hls_hamburger_menu),
        contentDescription = "Menu",
        contentScale = ContentScale.None,
        modifier = Modifier
            .clickable { onClick() }
    )
}

@Composable
fun HandRaiseButton(isHandRaised : Boolean, onClick: () -> Unit) {
    Image(painter = painterResource(id = if(isHandRaised) live.hms.roomkit.R.drawable.compose_hls_hand_on else live.hms.roomkit.R.drawable.compose_hls_hand_off),
        contentDescription = "Close",
        contentScale = ContentScale.None,
        modifier = Modifier
            .clickable { onClick() }
    )
}



@Composable
fun CloseButton(onCloseButtonClicked: () -> Unit) {
    Image(painter = painterResource(id = live.hms.roomkit.R.drawable.hls_close_button),
        contentDescription = "Close",
        contentScale = ContentScale.None,
        modifier = Modifier
            .clickable { onCloseButtonClicked() }
            .padding(Spacing0)
            .padding(Spacing1)
        )
}

@Composable
fun ClosedCaptionsButton(closedCaptionsToggleClicked: () -> Unit, closedCaptionsEnabled : Boolean) {
    Image(painter =
    painterResource(id = if(closedCaptionsEnabled) live.hms.roomkit.R.drawable.hls_closed_caption_button else live.hms.roomkit.R.drawable.hls_closed_captions_disabled),
        contentDescription = "Closed Captions",
        contentScale = ContentScale.None,
        modifier = Modifier
            .clickable { closedCaptionsToggleClicked() }
            .height(32.dp))
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun ChatUI(
    childFragmentManager: FragmentManager,
    chatViewModel: ChatViewModel,
    meetingViewModel: MeetingViewModel,
    pinnedMessageUiUseCase: PinnedMessageUiUseCase,
    chatAdapter: ChatAdapter,
    openPolls : () -> Unit
) {

    fun chatRelatedObservers(
        binding: LayoutChatMergeBinding, viewLifecycleOwner: LifecycleOwner
    ) = with(binding) {
        chatHamburgerMenu.setOnSingleClickListener(200L) {
            SessionOptionBottomSheet(
                onScreenShareClicked = {  },
                onBRBClicked = {  },
                onPeerListClicked = {
                    meetingViewModel.launchParticipantsFromHls.postValue(Unit)
                },
                onRaiseHandClicked = { meetingViewModel.toggleRaiseHand()},
                onNameChange = {  },
                showPolls = { openPolls() },
                onRecordingClicked = {},
                disableHandRaiseDisplay = true
            ).show(
                childFragmentManager, MeetingFragment.AudioSwitchBottomSheetTAG
            )
        }
        meetingViewModel.isHandRaised.observe(viewLifecycleOwner) {handRaised ->
            if(handRaised)
                handRaise.setImageResource(live.hms.roomkit.R.drawable.hand_off)
            else
                handRaise.setImageResource(live.hms.roomkit.R.drawable.hand_on)

        }
        handRaise.setOnClickListener {
            meetingViewModel.toggleRaiseHand()
        }
        sendToBackground.setOnSingleClickListener {
            RoleBasedChatBottomSheet.launch(childFragmentManager, chatViewModel)
        }
        meetingViewModel.initPrebuiltChatMessageRecipient.observe(viewLifecycleOwner) {
            chatViewModel.setInitialRecipient(it.first, it.second)
        }
        chatViewModel.currentlySelectedRecipientRbac.observe(viewLifecycleOwner) { recipient ->
            ChatRbacRecipientHandling().updateChipRecipientUI(sendToChipText, recipient)
        }
        meetingViewModel.messageIdsToHide.observe(viewLifecycleOwner) { messageIdsToHide ->
            chatViewModel.updateMessageHideList(messageIdsToHide)
        }
        meetingViewModel.currentBlockList.observe(viewLifecycleOwner) { chatBlockedPeerIdsList ->
            chatViewModel.updateBlockList(chatBlockedPeerIdsList)
        }
        pinnedMessageUiUseCase.init(
            pinnedMessagesRecyclerView,
            pinCloseButton,
            meetingViewModel::unPinMessage,
            meetingViewModel.isAllowedToPinMessages()
        )
        PauseChatUIUseCase().setChatPauseVisible(chatOptionsCard, meetingViewModel)
        ChatUseCase().initiate(
            chatViewModel.messages,
            meetingViewModel.chatPauseState,
            meetingViewModel.roleChange,
            meetingViewModel.currentBlockList,
            viewLifecycleOwner,
            chatAdapter,
            chatMessages,
            chatViewModel,
            meetingViewModel,
            emptyIndicator,
            iconSend,
            editTextMessage,
            userBlocked,
            chatPausedBy,
            chatPausedContainer,
            chatExtra,
            meetingViewModel.prebuiltInfoContainer::isChatEnabled,
            meetingViewModel::availableRecipientsForChat,
            chatViewModel::currentlySelectedRbacRecipient,
            chatViewModel.currentlySelectedRecipientRbac
        )
        iconSend.setOnSingleClickListener {
            val messageStr = editTextMessage.text.toString().trim()
            if (messageStr.isNotEmpty()) {
                chatViewModel.sendMessage(messageStr)
                editTextMessage.setText("")
            }
        }
        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }
        meetingViewModel.pinnedMessages.observe(viewLifecycleOwner) { pinnedMessages ->
            pinnedMessageUiUseCase.messagesUpdate(
                pinnedMessages, pinnedMessagesDisplay
            )
        }
        meetingViewModel.peerLeaveUpdate.observe(viewLifecycleOwner) {
            chatViewModel.updatePeerLeave(it)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    AndroidView({
        val view = LayoutInflater.from(context).inflate(live.hms.roomkit.R.layout.layout_chat_merge, null)
        with(LayoutChatMergeBinding.bind(view)) {
            applyTheme()
            chatRelatedObservers(this, lifecycleOwner)
        }
        view
    }, modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight())
}

@Composable
fun OrientationSwapper(
    landscape: @Composable (isLandcape : Boolean) -> Unit, portrait: @Composable (isLandcape : Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            landscape(true)
        }

        else -> {
            portrait(false)
        }
    }
}

@Composable
fun ForwardButton(isDisabled : Boolean, buttonClicked: () -> Unit) {
    BaseButton(buttonClicked = {if(!isDisabled) {
        buttonClicked()
    }}, id = if(!isDisabled) live.hms.roomkit.R.drawable.hls_forward_arrow else live.hms.roomkit.R.drawable.forward_arrow_dimmed, description = "Forward 15 seconds")
}
@Composable
fun RewindButton(buttonClicked : () -> Unit) {
    BaseButton(
        buttonClicked = buttonClicked,
        id = live.hms.roomkit.R.drawable.hls_backward_arrow,
        description = "Rewind ten seconds"
    )
}

@Composable
fun BaseButton(buttonClicked : () -> Unit, @DrawableRes id : Int, description : String) {
    Image(
        modifier = Modifier
            .clickable { buttonClicked() }
            .size(64.dp),
        painter = painterResource(id = id),
        contentDescription = description,
        contentScale = ContentScale.None
    )

}
@Composable
fun PlayPauseButton(buttonClicked : () -> Unit, isPlaying : Boolean?) {
    Image(
        modifier = Modifier
            .clickable { buttonClicked() }
            .size(64.dp),
        painter = painterResource(id = if(isPlaying == true) live.hms.roomkit.R.drawable.exo_styled_controls_pause else live.hms.roomkit.R.drawable.exo_styled_controls_play),
        contentDescription = "Play",
        contentScale = ContentScale.None
    )
}

@Composable
fun HlsChatIcon(chatEnabled : Boolean, buttonClicked: () -> Unit) {
    if(chatEnabled) {
        Image(painter =
        painterResource(id = live.hms.roomkit.R.drawable.hls_chat_off),
            contentDescription = "Chat Open",
            contentScale = ContentScale.None,
            modifier = Modifier
                .clickable { buttonClicked() }
                .height(32.dp))
    }
}

@Composable
fun PauseWhenLeaving(player : HmsHlsPlayer) {
    OnLifecycleEvent { _, event ->
        when(event)
        {
            Lifecycle.Event.ON_PAUSE -> {
                player.pause()
            }

            Lifecycle.Event.ON_RESUME -> {
                player.resume()
                player.seekToLivePosition()
            }

            else -> {}
        }
    }
}

@Composable
fun RemoveStatsWhenPaused(setPlayerStatsListener: (Boolean, HmsHlsPlayer) -> Unit, player: HmsHlsPlayer) {
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                setPlayerStatsListener(false, player)
            }

            Lifecycle.Event.ON_RESUME -> {
                setPlayerStatsListener(true, player)
            }

            else -> {}
        }
    }
}