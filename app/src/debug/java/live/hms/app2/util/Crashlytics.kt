package live.hms.app2.util

import android.util.Log
import java.lang.Exception


/**
 * Send a DEBUG log message to logcat and firebase.
 * @param tag Used to identify the source of a log message.  It usually identifies
 *        the class or activity where the log call occurs.
 * @param message The message you would like logged.
 */
fun crashlyticsLog(tag: String, message: String) {
  Log.d(tag, message)
}

fun crashlyticsException(e : Exception) {
  Log.d("Exception", "${e.message}")
}

fun crashlyticsCustomKey(key : String, value : String) {
  // No-op
}
