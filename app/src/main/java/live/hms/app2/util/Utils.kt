package live.hms.app2.util;

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import live.hms.video.utils.HMSLogger
import java.io.IOException
import java.io.InputStream


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
