package live.hms.app2.ui

import android.app.Application
import leakcanary.LeakCanary
import live.hms.app2.ui.settings.SettingsStore

class ConditionalApplicationCode {
    fun run(applicationContext: Application) {
        val settings = SettingsStore(applicationContext)
        val enabled = settings.isLeakCanaryEnabled
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = enabled)
        LeakCanary.showLeakDisplayActivityLauncherIcon(enabled)
    }
}