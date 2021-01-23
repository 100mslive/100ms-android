package live.hms.android100ms.util

import android.os.Looper

object ThreadUtils {
  fun checkIsOnMainThread() {
    check(Thread.currentThread() === Looper.getMainLooper().thread) {
      "Not on main thread!"
    }
  }
}