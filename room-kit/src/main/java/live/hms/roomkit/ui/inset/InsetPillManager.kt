package live.hms.roomkit.ui.inset

import android.content.Context
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import live.hms.roomkit.R
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.videoview.HMSVideoView


class InsetPillManager : View.OnClickListener, GestureDetector.OnDoubleTapListener,
    GestureDetector.OnGestureListener {


    private var rootView: InsetPill? = null
    private var hmsVideoView: HMSVideoView? = null


    fun show(context: Context, track: HMSVideoTrack) {

        val li = LayoutInflater.from(context)
        rootView = li.inflate(R.layout.inset_pill, null) as InsetPill
        val view = rootView ?: return
        //
        view.keepScreenOn = true

        hmsVideoView = view.findViewById(R.id.hms_video_view)

        hmsVideoView?.setOnClickListener(this)
        hmsVideoView?.addTrack(track)

        val gestureDetector = GestureDetectorCompat(context, this)
        gestureDetector.setOnDoubleTapListener(this)
        view.setGestureDetector(gestureDetector)

    }

    fun hide() {
        val view = rootView ?: return
        hmsVideoView?.removeTrack()
        view.close()
        hmsVideoView = null
        rootView = null
    }

    override fun onClick(v: View?) {
        //
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {

    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float
    ): Boolean {
        return false
    }
}