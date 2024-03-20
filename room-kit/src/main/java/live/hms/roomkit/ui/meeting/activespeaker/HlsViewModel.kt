package live.hms.roomkit.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import live.hms.hls_player.HmsHlsCue
import live.hms.hls_player.HmsHlsException
import live.hms.hls_player.HmsHlsPlaybackEvents
import live.hms.hls_player.HmsHlsPlaybackState
import live.hms.hls_player.HmsHlsPlayer
import live.hms.roomkit.util.SingleLiveEvent
import live.hms.video.sdk.HMSSDK

@UnstableApi class HlsViewModel(
    application: Application,
    private val hlsStreamUrl : String,
    private val hmsSdk: HMSSDK,
    private val hlsPlayerBeganToPlay : () -> Unit,
    private val displayHlsCuesUseCase : () -> DisplayHlsCuesUseCase
) : AndroidViewModel(application) {
    val isPlaying = MutableLiveData(true)
    val videoVisible = MutableLiveData(false)
    val progressBarVisible = videoVisible.map { !it }
    val isZoomEnabled = MutableLiveData(false)
    val isLive = MutableLiveData(true)
    val behindLiveByLiveData = MutableLiveData("0:0")
    val streamEndedEvent = SingleLiveEvent<Unit>()
    val currentSubtitles = MutableLiveData<String?>()
    private var failed = false

    val player = HmsHlsPlayer(application, hmsSdk).apply {
        setListeners(this)
        play(hlsStreamUrl)
    }
    fun areClosedCaptionsSupported() : Boolean =
        player.areClosedCaptionsSupported()

    fun restarted() {
        if(failed) {
            player.play(hlsStreamUrl)
            failed = false
        }
    }
    private fun setListeners(player: HmsHlsPlayer) {
//        binding.hlsView.player = player.getNativePlayer()
        player.getNativePlayer().addListener(@UnstableApi object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    videoVisible.postValue(true)
                }
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                super.onSurfaceSizeChanged(width, height)

            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
            }

            override fun onCues(cueGroup: CueGroup) {
                super.onCues(cueGroup)
                currentSubtitles.postValue(cueGroup.cues.firstOrNull()?.text?.toString())
            }
        })

        player.addPlayerEventListener(object : HmsHlsPlaybackEvents {

            override fun onPlaybackFailure(error: HmsHlsException) {
                Log.d("HMSHLSPLAYER", "From App, error: $error")
                failed = true
            }

            @SuppressLint("UnsafeOptInUsageError")
            override fun onPlaybackStateChanged(state: HmsHlsPlaybackState) {
//                contextSafe { context, activity ->
//                    activity.runOnUiThread {
                        if (state == HmsHlsPlaybackState.playing) {
                            hlsPlayerBeganToPlay()
                            isPlaying.postValue(true)
                        } else if (state == HmsHlsPlaybackState.stopped) {
                            // Open end stream fragment.
                            streamEndedEvent.postValue(Unit)
                            isPlaying.postValue(false)
                        } else isPlaying.postValue(true)
//                    }
//                }
//                Log.d("HMSHLSPLAYER", "From App, playback state: $state")
            }

            override fun onCue(cue: HmsHlsCue) {
                viewModelScope.launch {
                    displayHlsCuesUseCase().addCue(cue)

                }
            }
        })

    }
}