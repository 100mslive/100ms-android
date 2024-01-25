package live.hms.roomkit.ui.meeting.activespeaker

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM

@UnstableApi class HlsViewModel : ViewModel() {
    val isPlaying = MutableLiveData(true)
    val videoVisible = MutableLiveData<Boolean>(false)
    val progressBarVisible = videoVisible.map { !it }
    val resizeMode = MutableLiveData<Int>(RESIZE_MODE_ZOOM)
    val isLive = MutableLiveData(true)
}