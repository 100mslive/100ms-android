package live.hms.android100ms.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import live.hms.android100ms.BuildConfig
import java.util.*

object EmailUtils {

  const val TAG = "EmailUtils"

  fun getCrashLogIntent(context: Context, throwable: Throwable? = null): Intent {
    val logFile = LogUtils.saveLogsToFile(context, "nonfatal-log")
    val logUri = FileProvider.getUriForFile(context, "live.hms.android100ms.provider", logFile)

    val throwableStr = throwable?.let {
      "\n\n--------------------------------------------\n" +
          "Uncaught Exception: ${it.stackTraceToString()}" +
          "\n--------------------------------------------\n"
    } ?: ""

    val emailDescription = "Please explain the bug and steps to reproduce below:\n\n\n\n\n\n" +
        "NOTE: In case the logfile is not automatically attached with this email. " +
        "Find it in your device at '${logFile.absolutePath}'" +
        throwableStr +
        "\n\n--------------------------------------------\n" +
        "Device Information\n" +
        LogUtils.DEVICE_INFO.joinToString("\n") +
        "\n--------------------------------------------\n"

    Log.v(TAG, "Created intent with Email Description:\n\n$emailDescription")

    val to = BuildConfig.BUG_REPORT_EMAIL_TO.split(',').toTypedArray()
    val cc = BuildConfig.BUG_REPORT_EMAIL_CC.split(',').toTypedArray()

    return Intent(Intent.ACTION_SEND).apply {
      type = "vnd.android.cursor.dir/email"
      putExtra(Intent.EXTRA_STREAM, logUri)
      putExtra(Intent.EXTRA_EMAIL, to)
      putExtra(Intent.EXTRA_CC, cc)
      putExtra(Intent.EXTRA_SUBJECT, "Bug Report: 100ms Android App")
      putExtra(Intent.EXTRA_TEXT, emailDescription)

      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
  }
}