package live.hms.roomkit.ui.meeting

import android.app.Application
import android.os.HandlerThread
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.hms.roomkit.joins
import live.hms.roomkit.leaves
import live.hms.roomkit.tokens
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.video.events.AgentType
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.settings.HMSVideoTrackSettings
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.FrameworkInfo
import live.hms.video.sdk.models.HMSConfig
import live.hms.video.sdk.models.enums.HMSTrackUpdate

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

    val liveTrack = MutableLiveData<Pair<HMSTrack,HMSTrackUpdate>>()


    //create handler
    //create runnable
    private val safeHandlerThread by lazy { HandlerThread("leak-test").apply { start() } }
    private val safeHandler by lazy { android.os.Handler(safeHandlerThread.looper) }


    fun initSdk(hmsPrebuiltOptions: HMSPrebuiltOptions?, roomCode: String) {




        val tokenURL: String = hmsPrebuiltOptions?.endPoints?.get("token") ?: ""






            viewModelScope.launch {
                for (i in 1..200) {
                    try {
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
                        Log.d("LeakTest", "Token suceess $token")
                        hmsSDK.joins(HMSConfig(roomCode, token), liveTrack)
                        //random upto 2500
                        val random = (0..2500).random()
                        delay(random.toLong())
                        Log.d("LeakTest", "Join success $roomCode")
                        hmsSDK.leaves()
                        Log.d("LeakTest", "Leave success")

                    } catch (e: Exception) {
                        Log.d("LeakTest", "Error $e")
                    }
                }
            }


    }

}