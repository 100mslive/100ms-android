package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BehindLiveWindowException
import live.hms.app2.databinding.HlsFragmentLayoutBinding
import live.hms.app2.ui.meeting.HlsPlayer
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.video.utils.HMSLogger

class HlsFragment : Fragment() {

    private val TAG: String = "HlsFragment"
    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()

    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val hlsPlayer : HlsPlayer by lazy{
        HlsPlayer()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            hlsPlayer.mute(muted)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val exoPlayer = hlsPlayer.getPlayer(requireContext(),
        args.hlsStreamUrl,
        true)
        binding.hlsView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener{
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                HMSLogger.i(TAG, " ~~ Exoplayer error: $error")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                HMSLogger.i(TAG, "Playback state change to $playbackState")
            }
        })

    }

    private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = e.sourceException
        while (cause != null) {
            if (cause is BehindLiveWindowException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        hlsPlayer.releasePlayer()
    }
}