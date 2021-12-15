package live.hms.app2.ui.meeting

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


//https://qa-cdn.100ms.live/beam/618cee27b6c750e739e932a2/20211210/1639145185115/master.m3u8

class HlsPlayer {
    private val dataSourceFactory: DataSource.Factory
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var player : Player? = null

    init {
        dataSourceFactory = DefaultHttpDataSource.Factory()

    }

    fun getPlayer(context : Context, url : String = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8") : Player {

        val hlsMediaSource: HlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        val player = ExoPlayer.Builder(context).build()
        with(player) {
            setMediaSource(hlsMediaSource)
            playWhenReady = playWhenReady
            seekTo(currentWindow, playbackPosition)
            prepare()
        }
        this.player = player
        return player
    }

    fun releasePlayer() {
        player?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentMediaItemIndex
            playWhenReady = this.playWhenReady
            release()
        }
        player = null
    }
}