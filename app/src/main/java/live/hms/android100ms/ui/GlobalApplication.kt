package live.hms.android100ms.ui

import android.app.Application
import android.util.Log
import leakcanary.LeakCanary
import live.hms.android100ms.BuildConfig
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.util.crashlyticsLog

class GlobalApplication : Application() {

  companion object {
    const val TAG = "GlobalApplication"
  }

  override fun onCreate() {
    super.onCreate()
    Log.v(TAG, "onCreate()")

    if (BuildConfig.DEBUG) {
      val settings = SettingsStore(applicationContext)
      val enabled = settings.isLeakCanaryEnabled
      LeakCanary.config = LeakCanary.config.copy(dumpHeap = enabled)
      LeakCanary.showLeakDisplayActivityLauncherIcon(enabled)

      crashlyticsLog(TAG, "LeakCanary is enabled=$enabled")
    }
  }
}