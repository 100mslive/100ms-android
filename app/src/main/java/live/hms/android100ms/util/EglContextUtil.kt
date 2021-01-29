package live.hms.android100ms.util

import android.util.Log
import org.webrtc.EglBase

object EglContextUtil {

  private const val TAG = "EglContextUtil"
  private var count = 0

  private val provider: EglBase
  // val context: EglBase.Context

  init {
    Log.v(TAG, "Creating EglBase")
    provider = EglBase.create()
    // context = provider.eglBaseContext
  }

  val context: EglBase.Context
    get() {
      ++count
      Log.v(TAG, "Created $count context instances")
      return provider.eglBaseContext
    }
}