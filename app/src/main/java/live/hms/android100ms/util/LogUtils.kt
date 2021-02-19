package live.hms.android100ms.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


object LogUtils {
  /** Information about the current build, taken from system properties.  */
  val DEVICE_INFO = arrayOf(
    "Android SDK: ${Build.VERSION.SDK_INT}",
    "Release: ${Build.VERSION.RELEASE}",
    "Brand: ${Build.BRAND}",
    "Device: ${Build.DEVICE}",
    "Id: ${Build.ID}",
    "Hardware: ${Build.HARDWARE}",
    "Manufacturer: ${Build.MANUFACTURER}",
    "Model: ${Build.MODEL}",
    "Product: ${Build.PRODUCT}"
  )

  private val dateFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

  @JvmStatic
  fun logDeviceInfo(tag: String?) {
    Log.d(tag, DEVICE_INFO.joinToString(", "))
  }

  fun saveLogsToFile(context: Context, filename: String): File {
    val logsDir = File(context.getExternalFilesDir(null), "")
    val fileNameSuffix = Date().let { "${dateFormatter.format(it)}-${it.time}" }
    val logFile = File(logsDir, "$filename-$fileNameSuffix.log")

    crashlytics.recordException(Exception("Logs Update :)"))

    try {
      Runtime.getRuntime().exec(
        "logcat -f ${logFile.absolutePath}"
      )
    } catch (e: IOException) {
      Log.e(EmailUtils.TAG, "Error occurred while saving logs in ${logFile.absolutePath}", e)
    }

    Log.v(TAG, "Saved logs to file ${logFile.absolutePath}")

    return logFile
  }
}
