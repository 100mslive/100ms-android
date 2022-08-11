package live.hms.app2.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.FileProvider
import live.hms.app2.helpers.OnSingleClickListener
import live.hms.video.media.tracks.HMSVideoTrack
import java.io.File
import java.io.FileOutputStream


fun View.setOnSingleClickListener(l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l))
}

fun View.setOnSingleClickListener(l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l))
}

// Keep the listener at last such that we can use kotlin lambda
fun View.setOnSingleClickListener(waitDelay: Long, l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l, waitDelay))
}

fun View.setOnSingleClickListener(waitDelay: Long, l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l, waitDelay))
}


fun View.vibrateStrong() = reallyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

private fun View.reallyPerformHapticFeedback(feedbackConstant: Int) {
  if (context.isTouchExplorationEnabled()) {
    // Don't mess with a blind person's vibrations
    return
  }
  // Either this needs to be set to true, or android:hapticFeedbackEnabled="true" needs to be set in XML
  isHapticFeedbackEnabled = true

  // Most of the constants are off by default: for example, clicking on a button doesn't cause the phone to vibrate anymore
  // if we still want to access this vibration, we'll have to ignore the global settings on that.
  performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}

private fun Context.isTouchExplorationEnabled(): Boolean {
  // can be null during unit tests
  val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
  return accessibilityManager?.isTouchExplorationEnabled ?: false
}

fun Bitmap.saveCaptureToLocalCache(context: Context) : Uri? {
  kotlin.runCatching {

    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs() // don't forget to make the directory

    val stream =
      FileOutputStream("$cachePath/image.png") // overwrites this image every time

    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.close()

    val imagePath = File(context.cacheDir, "images")
    val newFile = File(imagePath, "image.png")
    return FileProvider.getUriForFile(context, "live.hms.app2.provider", newFile)

  }

  return null
}

fun Activity.openShareIntent(uri: Uri) {
    val type = contentResolver.getType(uri)
    val shareIntent = Intent().apply {
      action = Intent.ACTION_SEND
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      setDataAndType(uri,type )
      putExtra(Intent.EXTRA_STREAM, uri)
    }
    startActivity(Intent.createChooser(shareIntent, "Choose an app"))
}

fun HMSVideoTrack?.isValid(): Boolean {
 return !(this == null || this.isMute || this.isDegraded)
}


fun Context.showTileListDialog(
  peerName : String,
  onScreenCapture: (() -> Unit),
) {

  val builder = AlertDialog.Builder(this)
  builder.setTitle("Perform Action")
  val intentList = arrayOf("Screen Capture")
  builder.setItems(intentList) { dialog, which ->
    when (which) {
      0 -> { onScreenCapture.invoke() }
    }
  }

  builder.show()

}