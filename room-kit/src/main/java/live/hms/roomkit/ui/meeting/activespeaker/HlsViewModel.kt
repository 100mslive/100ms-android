package live.hms.roomkit.ui.meeting.activespeaker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi class HlsViewModel : ViewModel() {
    val isPlaying = MutableLiveData(true)
    val videoVisible = MutableLiveData(false)
    val progressBarVisible = videoVisible.map { !it }
    val resizeMode = MutableLiveData(RESIZE_MODE_FIT)
    val isLive = MutableLiveData(true)
    fun allowZoom() {
        viewModelScope.launch {
            delay(400)
            resizeMode.postValue(RESIZE_MODE_ZOOM)
        }
    }
}