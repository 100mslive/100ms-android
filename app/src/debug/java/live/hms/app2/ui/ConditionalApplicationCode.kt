package live.hms.app2.ui

import android.app.Application
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.crashlyticsLog

class ConditionalApplicationCode {
    fun run(applicationContext: Application) {
        val settings = SettingsStore(applicationContext)
        val enabled = settings.isLeakCanaryEnabled

        crashlyticsLog(GlobalApplication.TAG, "LeakCanary is enabled=$enabled")

    }
}