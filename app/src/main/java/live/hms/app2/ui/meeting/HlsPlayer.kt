package live.hms.app2.ui.meeting

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource


//https://qa-cdn.100ms.live/beam/618cee27b6c750e739e932a2/20211210/1639145185115/master.m3u8

class HlsPlayer {
    private val dataSourceFactory: DataSource.Factory
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var player : ExoPlayer? = null

    init {
        dataSourceFactory = DefaultHttpDataSource.Factory()
    }

    fun getPlayer() = player

    fun createPlayer(context : Context, url : String, playWhenready : Boolean = true) : ExoPlayer {

        val hlsMediaSource: HlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(url))

        val player = ExoPlayer.Builder(context).build()
        with(player) {
            setMediaSource(hlsMediaSource)
            playWhenReady = playWhenready
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

    fun mute(mute : Boolean) {
        player?.isDeviceMuted = mute
    }
}