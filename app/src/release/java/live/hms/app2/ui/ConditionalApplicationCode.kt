package live.hms.app2.ui

import android.app.Application

class ConditionalApplicationCode {

    fun run(application: Application) {
        crashlyticsLog(GlobalApplication.TAG, "LeakCanary is enabled=$enabled")
    }
}