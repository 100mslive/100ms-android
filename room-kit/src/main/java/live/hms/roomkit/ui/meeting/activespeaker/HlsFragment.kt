package live.hms.roomkit.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope

import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.launch
import live.hms.roomkit.databinding.HlsFragmentLayoutBinding
import live.hms.roomkit.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsCue
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsException
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsPlaybackEvents
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsPlaybackState
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsPlayer
import live.hms.roomkit.util.viewLifecycle
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.contextSafe
import live.hms.video.error.HMSException
import kotlin.math.absoluteValue

/**
 * If the stream is this many seconds behind live
 *  show the live buttons.
 */
private const val SECONDS_FROM_LIVE = 10
class HlsFragment : Fragment() {

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
        binding.applyTheme()
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

        setPlayerStatsListener(true)
    }


    override fun onStart() {
        super.onStart()
        resumePlay()
        player.play(args.hlsStreamUrl)
    }

    fun resumePlay() {

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
            override fun onPlaybackStateChanged(p1 : HmsHlsPlaybackState){
                contextSafe { context, activity ->
                    activity.runOnUiThread {

                    }
                }
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

    }



    override fun onStop() {
        super.onStop()
        player.stop()
    }
}