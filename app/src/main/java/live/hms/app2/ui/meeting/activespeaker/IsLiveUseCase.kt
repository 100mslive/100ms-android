package live.hms.app2.ui.meeting.activespeaker

import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.hms.hls_player.HlsPlayer

class IsLiveUseCase(private val player : HlsPlayer, private val lifecycleScope : LifecycleCoroutineScope, private val showLiveButton : (Boolean) -> Unit) : DefaultLifecycleObserver {
    private var timerJob : Job? = null

    fun monitorForLive(start : Boolean) {
        timerJob?.cancel()
        if(start) {
            timerJob = lifecycleScope.launch {
                while(true) {
                    delay(500)
                    val MILLISECONDS_BEHIND_LIVE_IS_PAUSED = 10 * 1000
                    val isLive =
                        (player.getNativePlayer().duration - player.getNativePlayer().currentPosition) - MILLISECONDS_BEHIND_LIVE_IS_PAUSED < 0
                    showLiveButton(!isLive)
                }
            }
        }
    }
}