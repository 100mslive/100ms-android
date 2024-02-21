package live.hms.roomkit.util;

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.C
import live.hms.video.utils.HMSLogger
import java.io.IOException
import java.io.InputStream
import java.util.Formatter


/**
 * Helper method which throws an exception  when an assertion has failed.
 */
fun assertIsTrue(condition: Boolean) {
  if (!condition) {
    throw AssertionError("Expected condition to be true");
  }
}

fun visibility(show: Boolean) = if (show) {
  View.VISIBLE
} else {
  View.GONE
}

fun visibilityOpacity(show: Boolean) = if (show) {
  1.0f
} else {
  0.0f
}


fun getBitmapFromAsset(context: Context, filename: String): Bitmap? {
  val assetManager = context.assets
  val istr: InputStream
  var bitmap: Bitmap? = null
  try {
    istr = assetManager.open(filename)
    bitmap = BitmapFactory.decodeStream(istr)
  } catch (e: IOException) {
    HMSLogger.e("videoPlugin", e.message + "error reading virtual background image")
  }

  return bitmap
}


fun Uri.getName(context: Context): String? {
  return try {
    val returnCursor = context.contentResolver.query(this, null, null, null, null)
    val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor.moveToFirst()
    val fileName = returnCursor.getString(nameIndex)
    returnCursor.close()
    fileName
  } catch (e: Exception) {
    null
  }
}

fun getStringForTime(builder: StringBuilder, formatter: Formatter, timeMs: Long): String {
  var timeMs = timeMs
  if (timeMs == C.TIME_UNSET) {
    timeMs = 0
  }
  val prefix = if (timeMs < 0) "-" else ""
  timeMs = Math.abs(timeMs)
  val totalSeconds = (timeMs + 500) / 1000
  val seconds = totalSeconds % 60
  val minutes = totalSeconds / 60 % 60
  val hours = totalSeconds / 3600
  builder.setLength(0)
  return if (hours > 0) formatter.format("%s%d:%02d:%02d", prefix, hours, minutes, seconds)
    .toString() else formatter.format("%s%02d:%02d", prefix, minutes, seconds).toString()
}

fun Fragment.contextSafe(funCall: (context: Context, activity: FragmentActivity) -> Unit) {
  if (context != null && activity != null && activity?.isFinishing == false && isAdded) {
     funCall.invoke(context!!, activity!!)
  }
}

