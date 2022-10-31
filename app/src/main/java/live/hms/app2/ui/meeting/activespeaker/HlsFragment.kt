package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import live.hms.app2.databinding.HlsFragmentLayoutBinding
import live.hms.app2.ui.meeting.HlsPlayer
import live.hms.app2.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.stats.PlayerEventsCollector
import live.hms.stats.PlayerEventsListener
import live.hms.stats.model.PlayerStats
import live.hms.video.utils.HMSLogger

class HlsFragment : Fragment() {

    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val playerUpdatesHandler = Handler()
    var runnable: Runnable? = null
    val TAG = "HlsFragment"
    var isStatsActive : Boolean = false
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val hlsPlayer: HlsPlayer by lazy {
        HlsPlayer()
    }
    var playerEventsManager: PlayerEventsCollector? = null

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
            hlsPlayer.getPlayer()?.seekToDefaultPosition()
            hlsPlayer.getPlayer()?.play()
        }

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            hlsPlayer.mute(muted)
        }

        meetingViewModel.statsToggleData.observe(viewLifecycleOwner) {

            if (it) {
                binding.statsViewParent.visibility = View.VISIBLE
                playerEventsManager?.addListener(object : PlayerEventsListener {
                    override fun onEventUpdate(playerStats: PlayerStats) {
                        binding.statsView.text = playerStats.toString()
                    }
                })
                isStatsActive = true
            } else {
                playerEventsManager?.removeListener()
                isStatsActive  = false
                binding.statsViewParent.visibility = View.GONE

            }

        }

        binding.btnTrackSelection.setOnClickListener {
            hlsPlayer.getPlayer()?.let {
                val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(it)
                trackSelectionBottomSheet.show(requireActivity().supportFragmentManager,"trackSelectionBottomSheet")
            }
        }

        hlsPlayer.getPlayer()?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                HMSLogger.i(TAG, " ~~ Exoplayer error: $error")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                HMSLogger.i(TAG, "Playback state change to $playbackState")
            }
        })


        runnable = Runnable {
            val distanceFromLive = ((hlsPlayer.getPlayer()?.duration?.minus(
                hlsPlayer.getPlayer()?.currentPosition ?: 0
            ))?.div(1000) ?: 0)

            HMSLogger.i(
                TAG,
                "duration : ${hlsPlayer.getPlayer()?.duration.toString()} current position ${hlsPlayer.getPlayer()?.currentPosition}"
            )
            HMSLogger.i(
                TAG,
                "buffered position : ${hlsPlayer.getPlayer()?.bufferedPosition}  total buffered duration : ${hlsPlayer.getPlayer()?.totalBufferedDuration} "
            )

            if (distanceFromLive >= 10) {
                binding.btnSeekLive.visibility = View.VISIBLE
            } else {
                binding.btnSeekLive.visibility = View.GONE
            }
            playerUpdatesHandler.postDelayed(runnable!!, 2000)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.hlsView.player = hlsPlayer.createPlayer(
            requireContext(),
            args.hlsStreamUrl,
            true
        )
        hlsPlayer.getPlayer()?.let {
            playerEventsManager = PlayerEventsCollector(it)
        }
        runnable?.let {
            playerUpdatesHandler.postDelayed(it, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        playerEventsManager?.removeListener()
    }

    override fun onResume() {
        super.onResume()
        if (isStatsActive) {
            playerEventsManager?.removeListener()
            playerEventsManager?.addListener(object : PlayerEventsListener {
                override fun onEventUpdate(playerStats: PlayerStats) {
                    binding.statsView.text = playerStats.toString()
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        hlsPlayer.releasePlayer()
        runnable?.let {
            playerUpdatesHandler.removeCallbacks(it)
        }
    }
}