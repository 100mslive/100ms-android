package live.hms.roomkit.ui.meeting

import android.app.Application
import android.graphics.Bitmap
import android.os.HandlerThread
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.hms.roomkit.addPlugin
import live.hms.roomkit.addPlugins
import live.hms.roomkit.joins
import live.hms.roomkit.leaves
import live.hms.roomkit.previews
import live.hms.roomkit.tokens
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.video.events.AgentType
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.settings.HMSVideoTrackSettings
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.plugin.video.utils.HMSBitmapPlugin
import live.hms.video.plugin.video.utils.HMSBitmapUpdateListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.FrameworkInfo
import live.hms.video.sdk.models.HMSConfig
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.virtualbackground.HMSVirtualBackground

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
                for (i in 1..50) {
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
                        val plugin = HMSVirtualBackground(hmsSDK)
                        Log.d("LeakTest", "Token suceess $token")

                        hmsSDK.joins(HMSConfig(roomCode, token), liveTrack)
                        Log.d("LeakTest", "Join success $roomCode")

                        hmsSDK.addPlugins(plugin)
                        Log.d("LeakTest", "Plugin success $roomCode")
                        plugin.enableBlur(100)
                        Log.d("LeakTest", "enabling blur $roomCode")

                        Log.d("LeakTest", "disabling blur $roomCode")

                        val random = (3000..5000).random()
                        delay(random.toLong())

                        hmsSDK.leaves()
                        Log.d("LeakTest", "Leave success")
                        if (i == 1) {
                            Log.d("LeakTest", "First iteration, waiting for 10 seconds")
                            delay(10000)
                        }

                    } catch (e: Exception) {
                        Log.d("LeakTest", "Error $e")
                    }
                }
            }


    }

    override fun onCleared() {
        super.onCleared()
    }

}