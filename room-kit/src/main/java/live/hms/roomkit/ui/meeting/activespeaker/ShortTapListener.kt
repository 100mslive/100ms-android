package live.hms.roomkit.ui.meeting.activespeaker

import android.view.GestureDetector
import android.view.MotionEvent

/**
 * If you just add a single tap listener to the surface view it triggers even when
 * there's a zoom ending. This avoids the issue.
 */
class ShortTapListener(private val action : () -> Unit) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        super.onSingleTapConfirmed(e)
        action()
        return true
    }
}