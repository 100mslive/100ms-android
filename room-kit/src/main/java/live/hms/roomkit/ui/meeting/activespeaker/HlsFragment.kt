package live.hms.roomkit.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.HlsFragmentLayoutBinding
import live.hms.roomkit.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.util.viewLifecycle
import live.hms.hls_player.*
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.ChatViewModelFactory
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
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
    private val chatViewModel: ChatViewModel by activityViewModels {
        ChatViewModelFactory(meetingViewModel.hmsSDK)
    }
    private val chatAdapter = ChatAdapter()

    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val TAG = "HlsFragment"
    var isStatsDisplayActive: Boolean = false
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    val player by lazy{ HmsHlsPlayer(requireContext(), meetingViewModel.hmsSDK) }
    val displayHlsCuesUseCase = DisplayHlsCuesUseCase { text -> binding.hlsCues.text = text }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSeekLive.setOnClickListener {
            player.seekToLivePosition()
        }

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            player.mute(muted)
        }


        meetingViewModel.statsToggleData.observe(viewLifecycleOwner) {
            isStatsDisplayActive = it
            setStatsVisibility(it)
        }

        binding.btnTrackSelection.setOnClickListener {
            binding.hlsView.let {
                val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(player)
                trackSelectionBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    "trackSelectionBottomSheet"
                )
            }
        }

        binding.iconSend.setOnSingleClickListener {
            val messageStr = binding.editTextMessage.text.toString().trim()
            if (messageStr.isNotEmpty()) {
                chatViewModel.sendMessage(messageStr)
                binding.editTextMessage.setText("")
            }
        }

        setPlayerStatsListener(true)

        with(binding.chatMessages) {
            // Set the adapter
            // Set the upside down recyclerview that starts items from the bottom
            // Fade the ends

            layoutManager = LinearLayoutManager(context)
                .apply {
                    reverseLayout = false
                    stackFromEnd = true
                }
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(140)
            adapter = chatAdapter
            recycledViewPool.setMaxRecycledViews(0, 0)
        }

        chatViewModel.messages.observe(viewLifecycleOwner) {
            chatAdapter.submitList(it)
            binding.chatMessages.postDelayed({
                binding.chatMessages.smoothScrollToPosition(it.size - 1)
            }, 300)
        }
    }

    private fun statsToString(playerStats: PlayerStatsModel): String {
        return "bitrate : ${Utils.humanReadableByteCount(playerStats.videoInfo.averageBitrate.toLong(),true,true)}/s \n" +
                "bufferedDuration  : ${playerStats.bufferedDuration.absoluteValue/1000} s \n" +
                "video width : ${playerStats.videoInfo.videoWidth} px \n" +
                "video height : ${playerStats.videoInfo.videoHeight} px \n" +
                "frame rate : ${playerStats.videoInfo.frameRate} fps \n" +
                "dropped frames : ${playerStats.frameInfo.droppedFrameCount} \n" +
                "distance from live edge : ${playerStats.distanceFromLive.div(1000)} s"
    }

    override fun onStart() {
        super.onStart()
        resumePlay()
        player.play(args.hlsStreamUrl)
    }

    fun resumePlay() {

        binding.hlsView.player = player.getNativePlayer()

        player.addPlayerEventListener(object : HmsHlsPlaybackEvents {

            override fun onPlaybackFailure(error : HmsHlsException) {
                Log.d("HMSHLSPLAYER","From App, error: $error")
            }

            override fun onPlaybackStateChanged(p1 : HmsHlsPlaybackState){
                Log.d("HMSHLSPLAYER","From App, playback state: $p1")
            }

            override fun onCue(hlsCue : HmsHlsCue) {
                viewLifecycleOwner.lifecycleScope.launch {
                    displayHlsCuesUseCase.addCue(hlsCue)
                }
            }
        })

    }

    override fun onPause() {
        super.onPause()
        setPlayerStatsListener(false)
    }

    override fun onResume() {
        super.onResume()
        if (isStatsDisplayActive) {
            setPlayerStatsListener(true)
        }
    }

    fun setStatsVisibility(enable: Boolean) {
        if(isStatsDisplayActive && enable) {
            binding.statsViewParent.visibility = View.VISIBLE
        } else {
            binding.statsViewParent.visibility = View.GONE
        }
    }
    private fun setPlayerStatsListener(enable : Boolean) {
        Log.d("SetPlayerStats","display: ${isStatsDisplayActive} && enable: ${enable}")

        if(enable) {
            player.setStatsMonitor(object : PlayerStatsListener {
                override fun onError(error: HMSException) {
                    Log.d(TAG,"Error $error")
                }

                @SuppressLint("SetTextI18n")
                override fun onEventUpdate(playerStats: PlayerStatsModel) {
                    updateLiveButtonVisibility(playerStats)
                    if(isStatsDisplayActive) {
                        updateStatsView(playerStats)
                    }
                }
            })
        } else {
            player.setStatsMonitor(null)
        }
    }

    fun updateStatsView(playerStats: PlayerStatsModel){
        binding.bandwidthEstimateTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.bandWidthEstimate, si = true, isBits = true)}/s"

        binding.networkActivityTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.totalBytesLoaded, si = true, isBits = true)}"

        binding.statsView.text = statsToString(playerStats)
    }

    fun updateLiveButtonVisibility(playerStats: PlayerStatsModel) {
        // It's live if the distance from the live edge is less than 10 seconds.
        val isLive = playerStats.distanceFromLive/1000 < SECONDS_FROM_LIVE
        // Show the button to go to live if it's not live.
        binding.btnSeekLive.visibility =  if(!isLive)
                View.VISIBLE
            else
                View.GONE
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }
}