package live.hms.android100ms.util

import android.util.Log
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun crashlyticsLog(tag: String, message: String) {
  // TODO: Disable firebase logs for release builds?
  FirebaseCrashlytics.getInstance().log("$tag: $message")
  Log.v(tag, message)
}

fun crashlyticsSetKey(key: String, value: String) {
  FirebaseCrashlytics.getInstance().setCustomKey(key, value)
}