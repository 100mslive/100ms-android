package live.hms.android100ms.util

import com.brytecam.lib.error.HMSException

fun toString(exception: HMSException): String {
  return "HMSException(" +
      "action=${exception.action}, " +
      "errorCode=${exception.errorCode}, " +
      "message=${exception.errorMessage})"
}

