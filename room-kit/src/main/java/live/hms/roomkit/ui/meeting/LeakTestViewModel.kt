package live.hms.roomkit.ui.meeting

import android.app.Application
import android.os.HandlerThread
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import live.hms.roomkit.joins
import live.hms.roomkit.leaves
import live.hms.roomkit.tokens
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.video.events.AgentType
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.settings.HMSVideoTrackSettings
import live.hms.video.media.settings.PhoneCallState
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.FrameworkInfo
import live.hms.video.sdk.models.HMSConfig

class LeakTestViewModel(
    application: Application
) : AndroidViewModel(application) {


    private val hmsTrackSettings = HMSTrackSettings.Builder()
        .audio(
            HMSAudioTrackSettings.Builder()
                .initialState(HMSTrackSettings.InitState.UNMUTED)
                .build()
        )
        .video(
            HMSVideoTrackSettings.Builder()
                .initialState(HMSTrackSettings.InitState.UNMUTED)
                .build()
        )
        .build()


    //create handler
    //create runnable
    private val safeHandlerThread by lazy { HandlerThread("leak-test").apply { start() } }
    private val safeHandler by lazy { android.os.Handler(safeHandlerThread.looper) }
    fun initSdk(hmsPrebuiltOptions: HMSPrebuiltOptions?, roomCode: String) {



        val tokenURL: String = hmsPrebuiltOptions?.endPoints?.get("token") ?: ""

        safeHandler.post {

            runBlocking {
                for (i in 0..100) {
                    val hmsSDK = HMSSDK.Builder(getApplication()).haltPreviewJoinForPermissionsRequest(true)
                        .setFrameworkInfo(
                            FrameworkInfo(
                                framework = AgentType.ANDROID_NATIVE, isPrebuilt = true
                            )
                        )
                        .setTrackSettings(hmsTrackSettings)
                        .build()
                    Log.d("LeakTest", "Iteration ${i} $roomCode")
                    val token = hmsSDK.tokens(roomCode)
                    hmsSDK.joins(HMSConfig(roomCode, token))
                    Log.d("LeakTest", "Join $roomCode")
                    hmsSDK.leaves()
                    delay(100)
                    Log.d("LeakTest", "Leave")
                }
            }
        }

    }

}