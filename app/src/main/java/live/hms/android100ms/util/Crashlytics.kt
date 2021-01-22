package live.hms.android100ms.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

val crashlytics = FirebaseCrashlytics.getInstance()

fun crashlyticsLog(tag: String, message: String) {
  // TODO: Disable firebase logs for release builds?
  crashlytics.log("$tag: $message")
  Log.v(tag, message)
}

