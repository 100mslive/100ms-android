package live.hms.roomkit.util

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class MediaPlayerManager(
    private val lifecycle: Lifecycle
) : LifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null

    init {
        lifecycle.addObserver(this)
    }

    private val onMediaPlayerPrepared = MediaPlayer.OnPreparedListener {
        try {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                && lifecycle.currentState != Lifecycle.State.DESTROYED
            )
                mediaPlayer?.start()
        } catch (e: Exception) {
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        releaseMediaPlayer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        releaseMediaPlayer()
    }

    private fun setSource(@RawRes raw: Int, context: Context) {
        try {
            mediaPlayer = MediaPlayer.create(context,raw).apply {
                start()
            }
        } catch (e: Exception) {
            val a = e
        }
    }

    private fun releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Public API
     */
    fun startPlay(@RawRes raw: Int, context: Context) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            && lifecycle.currentState != Lifecycle.State.DESTROYED
        ) {
            try {
                releaseMediaPlayer()
                setSource(raw, context)
            } catch (e: Exception) {
            }
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true)
            mediaPlayer?.pause()
    }

    fun resume() {
        if (mediaPlayer?.isPlaying?.not() == true)
            mediaPlayer?.start();
    }

    fun stop() {
        mediaPlayer?.stop()
    }
}