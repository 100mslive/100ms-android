package live.hms.roomkit.ui.meeting.activespeaker

import androidx.fragment.app.Fragment
/*

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import live.hms.hls_player.HmsHlsPlayer
import live.hms.roomkit.databinding.HlsFragmentLayoutBinding
import live.hms.roomkit.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.util.viewLifecycle
import live.hms.hls_player.*
import live.hms.roomkit.ui.meeting.bottomsheets.StreamEnded
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.contextSafe
import live.hms.stats.PlayerStatsListener
import live.hms.stats.Utils
import live.hms.stats.model.PlayerStatsModel
import live.hms.video.error.HMSException
import kotlin.math.absoluteValue
*/


/**
 * If the stream is this many seconds behind live
 *  show the live buttons.
 */
private const val SECONDS_FROM_LIVE = 10
class HlsFragment : Fragment() {

    /*private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val TAG = "HlsFragment"
    var isStatsDisplayActive: Boolean = false
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val player by lazy{ HmsHlsPlayer(requireContext(), meetingViewModel.hmsSDK) }
    val displayHlsCuesUseCase = DisplayHlsCuesUseCase( { text -> binding.hlsCues.text = text })
    { pollId ->
        lifecycleScope.launch {
            val hmsPoll = meetingViewModel.getPollForPollId(pollId)
            if(hmsPoll != null)
                meetingViewModel.triggerPollsNotification(hmsPoll)
        }
    }

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
        binding.applyTheme()

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            player.mute(muted)
        }


        meetingViewModel.statsToggleData.observe(viewLifecycleOwner) {
            isStatsDisplayActive = it
            setStatsVisibility(it)
        }

        binding.btnTrackSelection.setOnClickListener {
            fadeinOutTrackSelectionButton()
            if (binding.btnTrackSelection.alpha == 1f) {
                binding.hlsView.let {
                    val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(player)
                    trackSelectionBottomSheet.show(
                        requireActivity().supportFragmentManager,
                        "trackSelectionBottomSheet"
                    )
                }
            }
        }

        binding.hlsView.setOnTouchListener { v, event ->
            fadeinOutTrackSelectionButton()
            false
        }


        setPlayerStatsListener(true)
    }

    private fun fadeinOutTrackSelectionButton() {
        binding.btnTrackSelection.animate().cancel()
        binding.btnTrackSelection.alpha = 1f
        binding.btnTrackSelection.animate().alpha(0f).setStartDelay(3000).start()
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

    private fun resumePlay() {

        binding.hlsView.player = player.getNativePlayer()
        player.getNativePlayer().addListener(object : Player.Listener {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                super.onSurfaceSizeChanged(width, height)

            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                viewLifecycleOwner.lifecycleScope.launch {

                    if (videoSize.height !=0 && videoSize.width !=0) {
                        val width = videoSize.width
                        val height = videoSize.height

                        //landscape play
                        if (width > height) {
                            binding.hlsView.resizeMode = RESIZE_MODE_FIT
                        } else {
                            binding.hlsView.resizeMode = RESIZE_MODE_ZOOM
                        }
                        binding.progressBar.visibility = View.GONE
                        binding.hlsView.visibility = View.VISIBLE
                    }
                }

            }
        })
        
        player.addPlayerEventListener(object : HmsHlsPlaybackEvents {

            override fun onPlaybackFailure(error : HmsHlsException) {
                Log.d("HMSHLSPLAYER","From App, error: $error")
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onPlaybackStateChanged(state : HmsHlsPlaybackState){
                contextSafe { context, activity ->
                    activity.runOnUiThread {
                        if(state == HmsHlsPlaybackState.playing) {
                            meetingViewModel.hlsPlayerBeganToPlay()
                        }
                        else if (state == HmsHlsPlaybackState.stopped) {
                            // Open end stream fragment.
                            StreamEnded.launch(parentFragmentManager)
                        }
                    }
                }
                Log.d("HMSHLSPLAYER","From App, playback state: $state")
            }

            override fun onCue(cue : HmsHlsCue) {
                viewLifecycleOwner.lifecycleScope.launch {
                    displayHlsCuesUseCase.addCue(cue)
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
//                    updateLiveButtonVisibility(playerStats)
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
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }*/
}