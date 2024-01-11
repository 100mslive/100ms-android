package live.hms.roomkit.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.view.animation.LinearInterpolator
import androidx.core.content.FileProvider
import androidx.core.view.GestureDetectorCompat
import live.hms.roomkit.R
import live.hms.roomkit.helpers.OnSingleClickListener
import live.hms.roomkit.ui.notification.CardStackLayoutManager
import live.hms.roomkit.ui.notification.CardStackListener
import live.hms.roomkit.ui.notification.Direction
import live.hms.roomkit.ui.notification.StackFrom
import live.hms.roomkit.ui.notification.SwipeableMethod
import live.hms.video.media.capturers.camera.CameraControl
import live.hms.video.media.settings.HMSSimulcastLayerDefinition
import live.hms.video.media.tracks.HMSLocalVideoTrack

import live.hms.video.media.tracks.HMSRemoteVideoTrack
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.videoview.HMSVideoView
import org.webrtc.EglRenderer
import org.webrtc.SurfaceViewRenderer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt


fun View.setOnSingleClickListener(l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l))
}

private fun getDip(): Float = Resources.getSystem().displayMetrics.density
fun Float.dp() = this * getDip()
fun Int.dp() = (this * getDip()).roundToInt()

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
    return FileProvider.getUriForFile(context, "live.hms.roomkit.provider", newFile)

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
  isLocalTrack : Boolean,
  onScreenCapture: (() -> Unit),
  onSimulcast: (() -> Unit),
  onMirror: (() -> Unit)
) {

  val builder = AlertDialog.Builder(this)
  builder.setTitle("Perform Action")
  val intentList = mutableListOf("Screen Capture", "Mirror")
  if (isLocalTrack.not())
    intentList+= "Quality"
  builder.setItems(intentList.toTypedArray()) { _, which ->
    when (which) {
      0 -> { onScreenCapture.invoke() }
        1 -> {onMirror()}
      2 -> {
          onSimulcast.invoke()
      }
    }
  }

  builder.show()

}

fun SurfaceViewRenderer.onBitMap(onBitmap: (Bitmap?) -> Unit, scale : Float = 1.0f) {
  kotlin.runCatching {  }
  this.addFrameListener(object : EglRenderer.FrameListener{
    override fun onFrame(bitmap: Bitmap?) {
      onBitmap.invoke(bitmap)
      android.os.Handler(Looper.getMainLooper()).post { removeFrameListener(this) }
    }
  }, scale)
}

fun Context.showSimulcastDialog(hmsVideoTrack: HMSRemoteVideoTrack?) {
    if (hmsVideoTrack == null)
        return

    var selectedQualityIndex = 0
    val currentLayer = hmsVideoTrack.getLayer()


    val videoQuality = hmsVideoTrack.getLayerDefinition()?.map { "${it.layer} (${it.resolution.width} X ${it.resolution.height})" }?.toTypedArray().orEmpty()

    videoQuality.filterIndexed { index, quality ->
        if (quality.indexOf(currentLayer.toString()) != -1) {
            selectedQualityIndex = index
            true
        }
        else
            false
    }

    AlertDialog.Builder(this).apply {
             setTitle("Select Video Quality")
            .setSingleChoiceItems(videoQuality, selectedQualityIndex
            ) { dialog, which ->

              val layerDefinition : List<HMSSimulcastLayerDefinition> = hmsVideoTrack.getLayerDefinition()
              //safe check
              if (which>= layerDefinition.size)
                 return@setSingleChoiceItems

              hmsVideoTrack.setLayer(layerDefinition[which].layer)
            }
            show()
    }

}

fun Context.showMirrorOptions(surfaceViewRenderer: SurfaceViewRenderer?) {
    if(surfaceViewRenderer == null)
        return
    AlertDialog.Builder(this).apply {
        setTitle("Select mirror")
            .setSingleChoiceItems(arrayOf("Ignore","Mirror: True","Mirror: False"), 0) { _, which ->
                if(which == 1) {
                    Log.d("Mirroring","set mirror true")
                    surfaceViewRenderer.setMirror(true)
                } else if (which == 2) {
                    Log.d("Mirroring","set mirror false")
                    surfaceViewRenderer.setMirror(false)
                } else {
                    Log.d("Mirroring","don't set mirror")
                }
            }
        show()
    }
}


fun SurfaceViewRenderer.setInit() {
    setTag(R.id.IS_INT,true)
}

fun SurfaceViewRenderer.setRelease() {
    setTag(R.id.IS_INT,false)
}

fun SurfaceViewRenderer.isInit() : Boolean {
    return (getTag(R.id.IS_INT) as? Boolean) == true
}

fun HMSVideoTrack?.switchCamera() {
    (this as? HMSLocalVideoTrack)?.switchCamera()
}

fun HMSVideoView.setCameraGestureListener(track : HMSVideoTrack?,onImageCapture : (Uri)-> Unit, onLongPress: () -> Unit) {

    val cameraControl: CameraControl = (track as? HMSLocalVideoTrack)?.getCameraControl() ?: return
    var lastZoom = cameraControl.getMinZoom()

    val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            if (cameraControl.isTapToFocusSupported())
            cameraControl.setTapToFocusAt(
                 event.x,
                 event.y,
                viewWidth = width,
                viewHeight = height
            )
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {

            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageSavePath = File(cachePath, "image.jpeg")

            cameraControl.captureImageAtMaxSupportedResolution(imageSavePath) { it ->

                val fileSaveUri = FileProvider.getUriForFile(
                    context,
                    "live.hms.roomkit.provider",
                    imageSavePath
                )

                onImageCapture.invoke(fileSaveUri)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPress.invoke()
        }



    })

    val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (cameraControl.isZoomSupported()) {
                    lastZoom *= detector.scaleFactor
                    cameraControl.setZoom(lastZoom)
                    return true
                }
                return false
            }
        })

    this.setOnTouchListener { _, event ->
        var didConsume = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            didConsume = gestureDetector.onTouchEvent(event)
        }
        didConsume
    }




}


fun CardStackLayoutManager?.init(context: Context, listener: CardStackListener) : CardStackLayoutManager {
    var layoutManager = this
    if (layoutManager == null)
        layoutManager = CardStackLayoutManager(context, listener).apply {
            setStackFrom(StackFrom.Bottom)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(0f)
            setDirections(Direction.HORIZONTAL)
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }
    return layoutManager

}