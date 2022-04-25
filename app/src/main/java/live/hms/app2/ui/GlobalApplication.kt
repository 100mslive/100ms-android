package live.hms.app2.ui

import android.app.Application
import android.util.Log
import live.hms.video.utils.LogUtils

class GlobalApplication : Application() {

  companion object {
    const val TAG = "GlobalApplication"
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
  }
}