package live.hms.app2.util

import android.os.Looper

object ThreadUtils {
  fun checkIsOnMainThread() {
    check(Thread.currentThread() === Looper.getMainLooper().thread) {
      "Not on main thread!"
    }
  }

  /**
   * Helper method for building a string of thread information.
   */
  @JvmStatic
  fun getThreadInfo(): String {
    val thread = Thread.currentThread()
    return "@[name=${thread.name}, id=${thread.id}]"
  }
}