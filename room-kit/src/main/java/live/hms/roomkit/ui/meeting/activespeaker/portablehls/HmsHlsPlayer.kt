package live.hms.roomkit.ui.meeting.activespeaker.portablehls


import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import live.hms.video.media.settings.HMSVideoResolution
import live.hms.video.sdk.HMSSDK
import java.util.*
import java.util.concurrent.TimeUnit

public enum class HmsHlsPlaybackState {
    playing, stopped, paused, buffering, failed, unknown
}

public data class HmsHlsCue(
    val startDate: Date,
    val endDate: Date?,
    val payloadval : String?,
    val id: String? = null,
)

sealed class HmsHlsLayer {
    object AUTO : HmsHlsLayer()
    public data class LayerInfo internal constructor(
        val resolution: HMSVideoResolution,
        val bitrate: Int,
        internal val trackGroup: TrackGroup,
        internal val index: Int
    ) : HmsHlsLayer()

}

interface HmsHlsPlayerInterface {
    fun play(url: String)
    fun stop()

    // Meant to be internal
    fun setAnalytics(analytics: HMSSDK?)

    // To show player stats

    // Returns an exoplayer instance wrapped with this.
    fun pause()
    fun resume()

    // For range 0-100
    var volume : Int
    fun setHmsHlsLayer(hlsStreamVariant : HmsHlsLayer)
    fun getCurrentHmsHlsLayer() : HmsHlsLayer?
    fun getHmsHlsLayers() : List<HmsHlsLayer>
    // Seeking
    fun seekForward(value : Long, unit : TimeUnit)
    fun seekBackward(value : Long, unit : TimeUnit)
    fun seekToLivePosition()
    fun getLastError() : HmsHlsException? // TODO HMSException might need to be separated
    // TODO Callbacks https://www.notion.so/100ms/HMSHLSPlayer-V1-d1bea103f92c40c1ba921567fcdce738?pvs=4#3b9b75ca46764cc58a28e95f46c10267
    // If possible
    //availableVariant, onResolutionChanged
    fun addPlayerEventListener(events : HmsHlsPlaybackEvents?)
}

class HmsHlsPlayer(
    private val context: Context,
    private val hmssdk: HMSSDK? = null
) : HmsHlsPlayerInterface {
    /** If the current playback is this far behind live
     * it is considered paused.
     */
    var MILLISECONDS_BEHIND_LIVE_IS_PAUSED : Long = 10*1000



    // TODO consider handling pauses for the metadata as well
    private var hlsMetadataHandler: HlsMetadataHandler? = null
    val TAG = "HMSHLSPLAYER"
    private var hmsLastError : HmsHlsException? = null
    private var events : HmsHlsPlaybackEvents? = null

    fun getNativePlayer() : ExoPlayer {
        createPlayerIfRequired(context)
        return player!!
    }


    override fun getLastError(): HmsHlsException? = hmsLastError
    override fun addPlayerEventListener(events: HmsHlsPlaybackEvents?) {
        this.events = events
    }

    private fun createPlayerIfRequired(context : Context) {
        Log.d(TAG,"Is going to create player? ${player == null}")
        if(player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                setEventsListeners(this)
                gatherPlayerStatsForClients(this)
            }
        }
    }

    private fun gatherPlayerStatsForClients(exoPlayer: ExoPlayer) {
    }

    private fun setEventsListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                // One of:
//                Player.STATE_IDLE, Player.STATE_BUFFERING, Player.STATE_READY or Player.STATE_ENDED.
                // The transition from buffering to ready is important to show the progressbar
                //  or the ready to play thing.
                when(playbackState) {

                    Player.STATE_BUFFERING -> {
                        events?.onPlaybackStateChanged(HmsHlsPlaybackState.buffering)
                    }
                    Player.STATE_ENDED -> {
                        events?.onPlaybackStateChanged(HmsHlsPlaybackState.stopped)
                    }
                    Player.STATE_IDLE -> {
                        // TODO Consider if prepare is needed
                    }
                    Player.STATE_READY -> {
                        // If playwhen ready was true, this will cause it to play
                        // But the callback for this will instead be sent by the isPlayingChanged
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                // play/pause
                if(isPlaying) {
                    events?.onPlaybackStateChanged(HmsHlsPlaybackState.playing)
                } else {
                    // This might change as a result of buffering etc
//                    onPlaybackStateChanged?.invoke(HmsHlsPlaybackState.stopped)
                }

            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                if(error.errorCode != PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW){
                    events?.onPlaybackStateChanged(HmsHlsPlaybackState.failed)
                    sendError(HmsHlsException(error))
                }
            }

        })
    }

    override fun pause() {
        player?.play()
    }

    override fun resume() {
        Log.d(TAG,"Resuming")
        player?.play()
    }

    @setparam:androidx.annotation.IntRange(from = 0, to = 10)
    override var volume: Int
        get() = player?.let { (it.volume * 10).toInt() } ?: 0
        set(value) {
            player?.volume = value/10f
        }

    override fun getHmsHlsLayers(): List<HmsHlsLayer> {
        return player?.currentTracks?.groups?.find {
            it.type == C.TRACK_TYPE_VIDEO
        }?.let { trackGroupInfo ->
            val layers = mutableListOf<HmsHlsLayer>()
            for (index in 0 until trackGroupInfo.mediaTrackGroup.length) {
                if (trackGroupInfo.type == C.TRACK_TYPE_VIDEO) {
                    val format = trackGroupInfo.getTrackFormat(index)
                    layers.add(
                        HmsHlsLayer.LayerInfo(
                            HMSVideoResolution(format.width, format.height),
                            format.bitrate,
                            trackGroupInfo.mediaTrackGroup,
                            index
                        )
                    )
                }
            }
            layers
        }?.apply {
            add(HmsHlsLayer.AUTO)
        } ?: emptyList()
    }
    override fun setHmsHlsLayer(layer: HmsHlsLayer) {
        val params = when(layer) {
            is HmsHlsLayer.AUTO -> {
//                player?.trackSelectionParameters = DefaultTrackSelector.Parameters.getDefaults(context)
                player?.trackSelectionParameters?.buildUpon()
                    ?.clearOverrides()
                    ?.build()
            }
            is HmsHlsLayer.LayerInfo -> {
                val trackGroupInfo = player?.currentTracks?.groups?.find { it.type == C.TRACK_TYPE_VIDEO }?.mediaTrackGroup
                if(trackGroupInfo != null ) {
                    val params = player?.trackSelectionParameters?.buildUpon()
                        ?.addOverride(TrackSelectionOverride(trackGroupInfo, layer.index))
                        ?.build()
                    params
                } else null
            }
        }
        if(params != null ) {
            player?.trackSelectionParameters = params
        }
    }

    /**
     * May be null if the player wasn't initialized, will return a value otherwise.
     */
    override fun getCurrentHmsHlsLayer(): HmsHlsLayer? {
        val trackGroupInfo = player?.currentTracks?.groups?.find { it.type == C.TRACK_TYPE_VIDEO }
        if(trackGroupInfo != null ) {
            val selected : TrackSelectionOverride? = player?.trackSelectionParameters
                ?.overrides
                ?.getOrDefault(trackGroupInfo.mediaTrackGroup, null)

            val selectedIndex = selected?.trackIndices?.getOrNull(0)
            return if(selectedIndex != null && ( selectedIndex < selected.mediaTrackGroup.length ) ) {
                val selectedFormat : Format = selected.mediaTrackGroup.getFormat(selectedIndex)
                HmsHlsLayer.LayerInfo(
                    HMSVideoResolution(selectedFormat.width, selectedFormat.height),
                    selectedFormat.bitrate,
                    trackGroupInfo.mediaTrackGroup,
                    // Might be set to UNSET as in indexOf here but seems unlikely
                    trackGroupInfo.mediaTrackGroup.indexOf(selectedFormat)
                )
            } else
            {
                HmsHlsLayer.AUTO
            }
        }
        return null
    }

    override fun seekForward(value: Long, unit: TimeUnit) {
        with(player) {
            this?.seekTo(this.currentPosition + unit.toMillis(value))
        }
    }

    override fun seekBackward(value: Long, unit: TimeUnit) {
        with(player) {
            this?.seekTo(this.currentPosition - unit.toMillis(value))
        }
    }

    override fun seekToLivePosition() {
        player?.seekToDefaultPosition()
        resume()
    }

    override fun play(url: String){
        createPlayer(context, url, true)
            .play()
    }

    override fun stop() {
        player?.stop()
        releasePlayer()
    }

    override fun setAnalytics(analytics: HMSSDK?) {

    }


    private val dataSourceFactory: DataSource.Factory
    private var playbackPosition = 0L
    private var player : ExoPlayer? = null

    init {
        dataSourceFactory = DefaultHttpDataSource.Factory()
    }

    private fun createPlayer(context : Context, url : String, playWhenready : Boolean = true) : ExoPlayer {

        val hlsMediaSource: HlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(url))

        createPlayerIfRequired(context)
        with(player!!) {
            setMediaSource(hlsMediaSource)
            playWhenReady = playWhenready
            prepare()
            addAnalyticsListener(EventLogger(null))
            addHlsMetadataListener(this)
        }
        return player!!
    }

    private fun addHlsMetadataListener(player: ExoPlayer) {
        hlsMetadataHandler = HlsMetadataHandler(player){ localMetaDataModel ->
            Log.d(TAG,"$localMetaDataModel")
            events?.onCue(localMetaDataModel)
        }
        hlsMetadataHandler?.start()
    }

    private fun releasePlayer() {
        Log.d(TAG,"Stopping")
        hlsMetadataHandler?.stop()
        hlsMetadataHandler = null
        player?.run {
            playWhenReady = this.playWhenReady
            release()
        }
        player = null
    }

    fun mute(mute : Boolean) {
        player?.isDeviceMuted = mute
    }

    fun sendError(hmsError: HmsHlsException) {
        events?.onPlaybackFailure(hmsError)
        hmsLastError = hmsError
    }
}