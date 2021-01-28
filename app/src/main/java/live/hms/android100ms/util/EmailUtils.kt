package live.hms.android100ms.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import live.hms.android100ms.BuildConfig
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object EmailUtils {

  const val TAG = "EmailUtils"

  fun getCrashLogIntent(context: Context): Intent {
    val formatter = SimpleDateFormat("yyyy.MM.dd-G-'at'-HH:mm:ss-z", Locale.getDefault())
    val logFileName = "logcat-${formatter.format(Date())}.txt"

    val logsDir = File(context.getExternalFilesDir(null), "")
    val logFile = File(logsDir, logFileName)
    val logUri = FileProvider.getUriForFile(context, "live.hms.android100ms.provider", logFile)

    try {
      Runtime.getRuntime().exec(
        "logcat -f ${logFile.absolutePath}"
      )
    } catch (e: IOException) {
      Log.e(TAG, "Error occurred while saving logs in ${logFile.absolutePath}", e)
    }

    val emailDescription = "Please explain the bug and steps to reproduce below:\n\n\n\n\n\n" +
        "NOTE: In case the logfile is not automatically attached with this email. " +
        "Find it in your device at '${logFile.absolutePath}'" +

        "\n\n--------------------------------------------\n" +
        "Device Information\n" +
        "Device: ${Build.DEVICE}\n" +
        "Brand: ${Build.BRAND}\n" +
        "Model: ${Build.MODEL}\n" +
        "Product: ${Build.PRODUCT}\n" +
        "SDK Version: ${Build.VERSION.SDK_INT}" +
        "\n--------------------------------------------\n"

    Log.v(TAG, "Created intent with Email Description:\n\n$emailDescription")

    val to = BuildConfig.BUG_REPORT_EMAIL_TO.split(',').toTypedArray()
    val cc = BuildConfig.BUG_REPORT_EMAIL_CC.split(',').toTypedArray()

    return Intent(Intent.ACTION_SEND).apply {
      type = "vnd.android.cursor.dir/email";
      putExtra(Intent.EXTRA_STREAM, logUri)
      putExtra(Intent.EXTRA_EMAIL, to)
      putExtra(Intent.EXTRA_CC, cc)
      putExtra(Intent.EXTRA_SUBJECT, "Bug Report: Android 100ms Sample App")
      putExtra(Intent.EXTRA_TEXT, emailDescription)
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
  }
}