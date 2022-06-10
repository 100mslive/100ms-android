package live.hms.app2.ui

import android.app.Application
import android.util.Log
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSLogSettings
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.sdk.HMSSDK
import live.hms.video.services.LogAlarmManager
import live.hms.video.utils.LogUtils

class GlobalApplication : Application() {

  companion object {
    const val TAG = "GlobalApplication"
    lateinit var hmsSDK: HMSSDK
  }

  private fun initSaveLogsOnCrash() {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler() // Crashlytics Handler

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e(TAG, "CRASH", throwable)
      LogUtils.saveLogsToFile(applicationContext, "crash-log")
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }

  override fun onCreate() {
    initSaveLogsOnCrash()
    super.onCreate()
    // This has different values in release and debug in the two files:
    // app/src/release/java/live/hms/app2/ui/ConditionalApplicationCode.kt
    // and
    // app/src/debug/java/live/hms/app2/ui/ConditionalApplicationCode.kt
    ConditionalApplicationCode().run(this)

    val hmsTrackSettings = HMSTrackSettings.Builder()
      .audio(
        HMSAudioTrackSettings.Builder()
          .setUseHardwareAcousticEchoCanceler(false).build()
      )
      .build()

    val hmsLogSettings : HMSLogSettings = HMSLogSettings(LogAlarmManager.DEFAULT_DIR_SIZE,true)

    hmsSDK = HMSSDK
            .Builder(this)
      .setTrackSettings(hmsTrackSettings) // SDK uses HW echo cancellation, if nothing is set in builder
      .setLogSettings(hmsLogSettings)
      .build()
  }
}