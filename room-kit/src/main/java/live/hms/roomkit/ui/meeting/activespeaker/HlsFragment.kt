package live.hms.roomkit.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.navArgs
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import live.hms.hls_player.*
import live.hms.roomkit.databinding.HlsFragmentLayoutBinding
import live.hms.roomkit.databinding.LayoutChatMergeBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.PauseChatUIUseCase
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
import live.hms.roomkit.ui.meeting.compose.Variables.Companion.Spacing1
import live.hms.roomkit.ui.polls.leaderboard.millisToText
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.stats.PlayerStatsListener
import live.hms.stats.Utils
import live.hms.stats.model.PlayerStatsModel
import live.hms.video.error.HMSException
import kotlin.math.absoluteValue

/**
 * If the stream is this many seconds behind live
 *  show the live buttons.
 */
private const val SECONDS_FROM_LIVE = 10

class HlsFragment : Fragment() {
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val args: HlsFragmentArgs by navArgs()
    private val hlsViewModel: HlsViewModel by activityViewModels()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()
    private val pinnedMessageUiUseCase = PinnedMessageUiUseCase()
    private val launchMessageOptionsDialog = LaunchMessageOptionsDialog()
    private val chatAdapter by lazy {
        ChatAdapter(
            { message ->
                launchMessageOptionsDialog.launch(
                    meetingViewModel,
                    childFragmentManager, message
                )
            },
            {},
            { message -> MessageOptionsBottomSheet.showMessageOptions(meetingViewModel, message) })
    }

    val TAG = "HlsFragment"
    var isStatsDisplayActive: Boolean = false

    //    private val player by lazy{ HmsHlsPlayer(requireContext(), meetingViewModel.hmsSDK) }
    val displayHlsCuesUseCase = DisplayHlsCuesUseCase({ text -> binding.hlsCues.text = text })
    { pollId ->
        lifecycleScope.launch {
            val hmsPoll = meetingViewModel.getPollForPollId(pollId)
            if (hmsPoll != null)
                meetingViewModel.triggerPollsNotification(hmsPoll)
        }
    }

    private lateinit var composeView : ComposeView
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)
        composeView = binding.composeView
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var controlsVisible by remember { mutableStateOf(true) }

//                val controlsAlpha: Float by animateFloatAsState(
//                    targetValue = if (controlsVisible) 1f else 0f,
//                    animationSpec = tween(
//                        durationMillis = 3000,
//                        easing = LinearEasing,
//                    ), label = "control hiding alpha transition"
//                )

                val visibility by hlsViewModel.progressBarVisible.observeAsState()
                val player = remember {
                    HmsHlsPlayer(context, meetingViewModel.hmsSDK).apply {
                        resumePlay(this)
                        play(args.hlsStreamUrl)
                    }
                }

                if (visibility == true) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        color = PrimaryDefault
                    )
                } else {
                    Column {
                        HlsComposable(hlsViewModel, controlsVisible,
                            {controlsVisible = !controlsVisible},
                            context,
                            player){ showTrackSelection(player) }
                        ChatHeader(
                            "Tech talks",
                            meetingViewModel.getLogo(),
                            1200,
                            35 * 60 * 1000
                        )
                        ChatUI(childFragmentManager, chatViewModel, meetingViewModel, pinnedMessageUiUseCase, chatAdapter)
                    }
                }

                val muteState by meetingViewModel.showAudioMuted.observeAsState()
                player.mute(muteState ?: false)

                OnLifecycleEvent{
                    _, event ->
                    when(event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            setPlayerStatsListener(false, player)
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            if (isStatsDisplayActive) {
                                setPlayerStatsListener(true, player)
                            }

                        }
                        else -> {}
                    }
                }


            }
        }
//        chatRelatedObservers()
        statsObservers()

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
                playerStats.videoInfo.averageBitrate.toLong(),
                true,
                true
            )
        }/s \n" +
                "bufferedDuration  : ${playerStats.bufferedDuration.absoluteValue / 1000} s \n" +
                "video width : ${playerStats.videoInfo.videoWidth} px \n" +
                "video height : ${playerStats.videoInfo.videoHeight} px \n" +
                "frame rate : ${playerStats.videoInfo.frameRate} fps \n" +
                "dropped frames : ${playerStats.frameInfo.droppedFrameCount} \n" +
                "distance from live edge : ${playerStats.distanceFromLive.div(1000)} s"
    }

    private fun resumePlay(player: HmsHlsPlayer) {
//        binding.hlsView.player = player.getNativePlayer()
        player.getNativePlayer().addListener(@UnstableApi object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    hlsViewModel.videoVisible.postValue(true)
                }
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                super.onSurfaceSizeChanged(width, height)

            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                viewLifecycleOwner.lifecycleScope.launch {

                    if (videoSize.height != 0 && videoSize.width != 0) {
                        val width = videoSize.width
                        val height = videoSize.height

                        //landscape play
                        if (width > height) {
                            hlsViewModel.resizeMode.postValue(RESIZE_MODE_FIT)
//                            binding.hlsView.resizeMode = RESIZE_MODE_FIT
                        } else {
                            hlsViewModel.resizeMode.postValue(RESIZE_MODE_ZOOM)
//                            binding.hlsView.resizeMode = RESIZE_MODE_ZOOM
                        }
//                        binding.progressBar.visibility = View.GONE
//                        binding.hlsView.visibility = View.VISIBLE
                    }
                }

            }
        })

        player.addPlayerEventListener(object : HmsHlsPlaybackEvents {

            override fun onPlaybackFailure(error: HmsHlsException) {
                Log.d("HMSHLSPLAYER", "From App, error: $error")
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onPlaybackStateChanged(state: HmsHlsPlaybackState) {
                contextSafe { context, activity ->
                    activity.runOnUiThread {
                        if (state == HmsHlsPlaybackState.playing) {
                            meetingViewModel.hlsPlayerBeganToPlay()
                        } else if (state == HmsHlsPlaybackState.stopped) {
                            // Open end stream fragment.
                            StreamEnded.launch(parentFragmentManager)
                        }
                    }
                }
                Log.d("HMSHLSPLAYER", "From App, playback state: $state")
            }

            override fun onCue(cue: HmsHlsCue) {
                viewLifecycleOwner.lifecycleScope.launch {
                    displayHlsCuesUseCase.addCue(cue)
                }
            }
        })

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
//                    updateLiveButtonVisibility(playerStats)
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
                playerStats.bandwidth.bandWidthEstimate,
                si = true,
                isBits = true
            )
        }/s"

        binding.networkActivityTv.text = "${
            Utils.humanReadableByteCount(
                playerStats.bandwidth.totalBytesLoaded,
                si = true,
                isBits = true
            )
        }"

        binding.statsView.text = statsToString(playerStats)
    }

    fun updateLiveButtonVisibility(playerStats: PlayerStatsModel) {
        // It's live if the distance from the live edge is less than 10 seconds.
        val isLive = playerStats.distanceFromLive / 1000 < SECONDS_FROM_LIVE
        // Show the button to go to live if it's not live.
    }

    private fun showTrackSelection(player: HmsHlsPlayer) {
        val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(player)
        trackSelectionBottomSheet.show(
            requireActivity().supportFragmentManager,
            "trackSelectionBottomSheet"
        )
    }
}

@Composable
fun ChatHeader(headingText: String, logoUrl: String?, viewers: Long, startedMillis: Long) {
    fun getViewersDisplayNum(viewers: Long): String =
        if (viewers < 1000) {
            "$viewers"
        } else
            "${viewers / 1000f}K"

    fun getTimeDisplayNum(startedMillis: Long): String =
        millisToText(startedMillis, false, "s")

    Row(
        Modifier
            .fillMaxWidth()
            .padding(Variables.Spacing2),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = logoUrl,
            placeholder = if (LocalInspectionMode.current) painterResource(id = R.drawable.exo_edit_mode_logo) else null,
            contentDescription = "Logo"
        )
        Column {
            Text(
                headingText, style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                    fontWeight = FontWeight(600),
                    color = Variables.OnSecondaryHigh,
                    letterSpacing = 0.1.sp,
                )
            )
            Text(
                "${getViewersDisplayNum(viewers)} watching ● Started ${
                    getTimeDisplayNum(
                        startedMillis
                    )
                } ago",
                style = TextStyle(
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
}

@Preview
@Composable
fun ChatHeaderPreview() {
    ChatHeader(
        headingText = "Tech talks",
        "https://storage.googleapis.com/100ms-cms-prod/cms/100ms_18a29f69f2/100ms_18a29f69f2.png",
        1000,
        30 * 60 * 1000
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
                name = it.senderName,
                message = it.message
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
            name,
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(live.hms.roomkit.R.font.inter_regular)),
                fontWeight = FontWeight(600),
                color = Variables.OnSurfaceHigh,
                letterSpacing = 0.1.sp,
            )
        )
        Text(
            message,
            style = TextStyle(
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
fun SettingsButton(
    onClickAction: () -> Unit
) {
    Image(
        painter = painterResource(id = live.hms.roomkit.R.drawable.settings),
        contentDescription = "Layer Select",
        modifier = Modifier
            .clickable { onClickAction() }
            .wrapContentSize()
            .padding(16.dp)
    )
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

@UnstableApi
@Composable
fun HlsComposable(hlsViewModel : HlsViewModel,
                  controlsVisible : Boolean,
                  videoTapped : () -> Unit,
                  context : Context,
                  player : HmsHlsPlayer,
                  settingsButtonTapped : () -> Unit
) {
    Box(contentAlignment = TopEnd) {

        AndroidView(
            modifier = Modifier
                .aspectRatio(ratio = 16f / 9)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            videoTapped()
                        }
                    )
                }
                .fillMaxWidth(),
            factory = {
                PlayerView(context)
                    .apply {
                        useController = false
                        resizeMode =
                            hlsViewModel.resizeMode.value ?: RESIZE_MODE_FIT
                        this.player = player.getNativePlayer()
                    }
            },
            update = {
                it.resizeMode = hlsViewModel.resizeMode.value ?: RESIZE_MODE_FIT
            })

        androidx.compose.animation.AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(2000)),
            exit = fadeOut(animationSpec = tween(2000))
        ) {
            SettingsButton(settingsButtonTapped)
        }

    }
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
fun ChatUI(childFragmentManager : FragmentManager,
           chatViewModel: ChatViewModel,
           meetingViewModel : MeetingViewModel,
           pinnedMessageUiUseCase : PinnedMessageUiUseCase,
           chatAdapter : ChatAdapter) {

    fun chatRelatedObservers(binding: LayoutChatMergeBinding,
                                     viewLifecycleOwner : LifecycleOwner
                                     ) = with(binding){
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
                pinnedMessages,
                pinnedMessagesDisplay
            )
        }
        meetingViewModel.peerLeaveUpdate.observe(viewLifecycleOwner) {
            chatViewModel.updatePeerLeave(it)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    AndroidView({
        LayoutInflater.from(context).inflate(live.hms.roomkit.R.layout.layout_chat_merge, null)
    },
        modifier = Modifier.fillMaxWidth().fillMaxHeight()) { view ->
        with(LayoutChatMergeBinding.bind(view)) {
            applyTheme()
            chatRelatedObservers(this,lifecycleOwner )
        }
    }
}
