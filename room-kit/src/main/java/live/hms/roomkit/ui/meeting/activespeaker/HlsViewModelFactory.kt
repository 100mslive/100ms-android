package live.hms.roomkit.ui.meeting.activespeaker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
//import androidx.media3.common.util.UnstableApi
import live.hms.video.sdk.HMSSDK

//@UnstableApi class HlsViewModelFactory(
//    private val application: Application,
//    private val hlsStreamUrl: String,
//    private val hmsSdk: HMSSDK,
//    private val hlsPlayerBeganToPlay: () -> Unit,
//    private val displayHlsCuesUseCase: () -> DisplayHlsCuesUseCase
//) : ViewModelProvider.NewInstanceFactory() {
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(HlsViewModel::class.java)) {
//            return HlsViewModel(application,hlsStreamUrl, hmsSdk, hlsPlayerBeganToPlay, displayHlsCuesUseCase) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
//    }
//}