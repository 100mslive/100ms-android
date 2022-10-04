package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.video.utils.HMSLogger

class HlsFragment : Fragment() {

    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val playerUpdatesHandler = Handler()
    var runnable: Runnable? = null
    val TAG = "HlsFragment"
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val hlsPlayer: HlsPlayer by lazy {
        HlsPlayer()
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

        binding.btnSeekLive.setOnClickListener {
            hlsPlayer.getPlayer()?.seekToDefaultPosition()
            hlsPlayer.getPlayer()?.play()
        }

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            hlsPlayer.mute(muted)
        }

        hlsPlayer.getPlayer()?.addListener(object : Player.Listener{
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

            HMSLogger.i(TAG,"duration : ${hlsPlayer.getPlayer()?.duration.toString()} current position ${hlsPlayer.getPlayer()?.currentPosition}")
            HMSLogger.i(TAG,"buffered position : ${hlsPlayer.getPlayer()?.bufferedPosition}  total buffered duration : ${hlsPlayer.getPlayer()?.totalBufferedDuration} ")

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
        runnable?.let {
            playerUpdatesHandler.postDelayed(it,0)
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