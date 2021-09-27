package live.hms.app2.util

import android.content.Context
import android.os.Build
import android.util.Log
import live.hms.app2.ui.settings.SettingsStore
import live.hms.video.utils.HMSLogger
import live.hms.video.utils.HMSUtils
import org.webrtc.RTCStats
import java.io.File
import java.io.FileWriter
import java.io.IOException
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

  private val logFileNameDateFormatter = SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSS", Locale.ENGLISH)
  private val logDateFormatter = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

  @JvmStatic
  fun logDeviceInfo(tag: String?) {
    Log.d(tag, DEVICE_INFO.joinToString(", "))
  }

  private const val TAG = "LogUtils"

  var currentSessionFile: File? = null
  var currentSessionFileWriter: FileWriter? = null

  fun staticFileWriterStart(context: Context, roomId: String) {
    currentSessionFileWriter?.let { writer ->
      HMSLogger.removeInjectedLoggable()
      writer.close()
      currentSessionFile = null
      currentSessionFileWriter = null
    }

    val settings = SettingsStore(context)
    HMSLogger.webRtcLogLevel = settings.logLevelWebrtc
    HMSLogger.level = settings.logLevel100msSdk

    currentSessionFile = makeLogFile(context, "session-log-${roomId}")
    val fileWriter = FileWriter(currentSessionFile)
    currentSessionFileWriter = fileWriter

    fileWriter.write("Android Agent: ${HMSUtils.getUserAgent()}")
    fileWriter.write("Device Info: ${DEVICE_INFO.joinToString("\n")}")

    HMSLogger.injectLoggable(object : HMSLogger.Loggable {
      override fun onLogMessage(
        level: HMSLogger.LogLevel,
        tag: String,
        message: String,
        isWebRtCLog: Boolean
      ) {
        val time = Date().let { logDateFormatter.format(it) }
        val prefix = "[${if (isWebRtCLog) "RTC" else "HMS"}:$level:$tag:$time]"
        fileWriter.write("$prefix\t\t${message.trimEnd()}\n")
      }

      override fun onLogToFile(
        fileName: HMSLogger.LogFiles,
        tag: String,
        message: MutableMap<String, RTCStats>
      ) {
        super.onLogToFile(fileName, tag, message)

      }
    })
  }

  private fun makeLogFile(context: Context, filename: String): File {
    val logsDir = File(context.getExternalFilesDir(null), "")
    val fileNameSuffix = Date().let { "${logFileNameDateFormatter.format(it)}-${it.time}" }

    return File(logsDir, "$filename-$fileNameSuffix.log")
  }

  fun saveLogsToFile(context: Context, filename: String): File {
    val logFile = makeLogFile(context, filename)

    try {
      Runtime.getRuntime().exec(
        "logcat -f ${logFile.absolutePath}"
      )
    } catch (e: IOException) {
      Log.e(TAG, "Error occurred while saving logs in ${logFile.absolutePath}", e)
    }

    Log.v(TAG, "Saved logs to file ${logFile.absolutePath}")

    return logFile
  }
}
