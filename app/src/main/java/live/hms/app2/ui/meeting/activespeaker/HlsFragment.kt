package live.hms.app2.ui.meeting.activespeaker

import android.media.metrics.PlaybackErrorEvent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackException
//import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.PlaybackStats
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation
import com.google.android.exoplayer2.video.VideoSize
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSeekLive.setOnClickListener {
            hlsPlayer.getPlayer()?.seekToDefaultPosition()
            hlsPlayer.getPlayer()?.play()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.hlsView.player = hlsPlayer.createPlayer(requireContext(),
                args.hlsStreamUrl,
                true)

        hlsPlayer.getPlayer()?.addListener(object : Player.Listener{
            override fun onPlayerError(error: PlaybackException) {
                HMSLogger.i(TAG, "Exoplayer error :: ${error.errorCodeName}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                HMSLogger.i(TAG, "Playback state change to $playbackState")
            }
        })

        hlsPlayer.getPlayer()?.addAnalyticsListener(object : AnalyticsListener{

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                HMSLogger.i(TAG,"bitrateEstimate :: ${humanReadableByteCount(bitrateEstimate.toInt(), true, true)}" +
                        ", bytesDownloaded :: $totalBytesLoaded")
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                HMSLogger.i(TAG, "Dropped $droppedFrames frames")
            }

            override fun onVideoSizeChanged(
                eventTime: AnalyticsListener.EventTime,
                videoSize: VideoSize
            ) {
                HMSLogger.i(TAG, "Video width: ${videoSize.width}, height: ${videoSize.height}")
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                HMSLogger.i(TAG, "video codec :: ${format.codecs}, average bitrate :: ${humanReadableByteCount(format.bitrate, true, true)}")
                HMSLogger.i(TAG, "video width :: ${format.width}, video height :: ${format.height}, video framerate :: ${format.frameRate}")
                HMSLogger.i(TAG, "video MIME type :: ${format.sampleMimeType}")
            }

        })

        hlsPlayer.getPlayer()?.addAnalyticsListener(PlaybackStatsListener(false
        ) { eventTime, playbackStats ->
            HMSLogger.i(TAG,"eventTime :: $eventTime, playbackStats :: $playbackStats")
        })
        hlsPlayer.getPlayer()?.bufferedPosition

        hlsPlayer.startPlayer(args.hlsStreamUrl, true)
    }

    fun humanReadableByteCount(bytes: Int, si: Boolean, isBits: Boolean): String {
        val unit = if (!si) 1000 else 1024
        if (bytes < unit) return "$bytes KB"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1]
            .toString() + if (si) "" else "i"
        return if (isBits) String.format(
            "%.1f %sb",
            bytes / Math.pow(unit.toDouble(), exp.toDouble()),
            pre
        ) else String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    override fun onStop() {
        super.onStop()
        hlsPlayer.releasePlayer()
    }
}