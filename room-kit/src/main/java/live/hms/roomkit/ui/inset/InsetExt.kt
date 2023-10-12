package live.hms.roomkit.ui.inset


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Resources
import android.util.Log
import android.view.*
import androidx.core.view.GestureDetectorCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import live.hms.roomkit.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val DRAG_TOLERANCE = 16
const val DURATION_MILLIS = 250L

internal fun View.marginStart(): Float {
    return ((layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0).toFloat()
}

internal fun View.marginEnd(): Float {
    return ((layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd ?: 0).toFloat()
}

internal fun View.marginTop(): Float {
    return ((layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0).toFloat()
}

internal fun View.marginBottom(): Float {
    return ((layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0).toFloat()
}

enum class Mode {
    NON_STICKY, STICKY_X, STICKY_Y, STICKY_XY
}

internal data class InsetViewState(
    var isMoving: Boolean,
    var isLongPressRegistered: Boolean,
)


internal fun View.makeInset(
//  minimizeBtnListener: DraggableView.Listener,
    stickyAxis: Mode = Mode.STICKY_XY,
    animated: Boolean = true,
    onDoubleTap: () -> Unit,
//    draggableListener: DraggableListener? = null,
) {
    var widgetInitialX = 0f
    var widgetDX = 0f
    var widgetInitialY = 0f
    var widgetDY = 0f

    val marginStart = marginStart()
    val marginTop = marginTop()
    val marginEnd = marginEnd()
    val marginBottom = marginBottom()

    val viewState = InsetViewState(
        isMoving = false, isLongPressRegistered = false
    )
    val gestureDetector =
        GestureDetectorCompat(this.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (viewState.isMoving) return
                viewState.isLongPressRegistered = true
//                draggableListener?.onLongPress(this@setupDraggable)
            }



            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onDoubleTap.invoke()
                return super.onSingleTapConfirmed(e)
            }
        })

    setOnTouchListener { v, event ->
        val viewParent = v.parent as View
        val parentHeight = viewParent.height
        val parentWidth = viewParent.width
        val xMax = parentWidth - v.width - marginEnd
        val xMiddle = parentWidth / 2
        val yMax = parentHeight - v.height - marginBottom
        val yMiddle = parentHeight / 2

        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                viewState.isLongPressRegistered = false
                widgetDX = v.x - event.rawX
                widgetDY = v.y - event.rawY
                widgetInitialX = v.x
                widgetInitialY = v.y
            }

            MotionEvent.ACTION_MOVE -> {
                var newX = event.rawX + widgetDX
                newX = max(marginStart, newX)
                newX = min(xMax, newX)
                if (abs(v.x - newX) > DRAG_TOLERANCE) viewState.isMoving = true
                v.x = newX

                var newY = event.rawY + widgetDY
                newY = max(marginTop, newY)
                newY = min(yMax, newY)
                if (abs(v.y - newY) > DRAG_TOLERANCE) viewState.isMoving = true
                v.y = newY

//                draggableListener?.onPositionChanged(v)
//                minimizeBtnListener.onPositionChanged(v, StickyRestSide.HIDE)
            }

            MotionEvent.ACTION_UP -> {
                viewState.isMoving = false
                when (stickyAxis) {
                    Mode.STICKY_X -> {
                        if (event.rawX >= xMiddle) {
                            if (animated) v.animate().x(xMax).setDuration(DURATION_MILLIS)
                                .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
//                                        minimizeBtnListener.onPositionChanged(v, StickyRestSide.RIGHT)
                                }.setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        super.onAnimationEnd(animation)
                                        Log.d("drg", "Animate END Sticky X RIGHT")
                                    }
                                }).start()
                            else {
                                v.x = xMax
//                                minimizeBtnListener.onPositionChanged(v, StickyRestSide.RIGHT)
                            }
                        } else {
                            if (animated) v.animate().x(marginStart).setDuration(DURATION_MILLIS)
                                .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
//                                        minimizeBtnListener.onPositionChanged(v, StickyRestSide.LEFT)
                                }.start()
                            else {
                                v.x = marginStart
//                                minimizeBtnListener.onPositionChanged(v, StickyRestSide.LEFT)
                            }
                        }
                    }

                    Mode.STICKY_Y -> {
                        if (event.rawY >= yMiddle) {
                            if (animated) v.animate().y(yMax).setDuration(DURATION_MILLIS)
                                .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
//                                        minimizeBtnListener.onPositionChanged(v, StickyRestSide.BOTTOM)
                                }.start()
                            else v.y = yMax
                        } else {
                            if (animated) v.animate().y(marginTop).setDuration(DURATION_MILLIS)
                                .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
                                }.start()
                            else v.y = marginTop
                        }
                    }

                    Mode.STICKY_XY -> {
                        resetUI(event.rawX, event.rawY)
                    }

                    else -> {
                    }
                }

                if (viewState.isLongPressRegistered) {
                    // Don't register click
                    return@setOnTouchListener true
                }

                if (abs(v.x - widgetInitialX) <= DRAG_TOLERANCE && abs(v.y - widgetInitialY) <= DRAG_TOLERANCE) {
                    performClick()
                }
            }

            else -> return@setOnTouchListener false
        }
        true
    }
}

internal fun View.resetUI(currentX : Float,  currentY : Float, animated: Boolean = true) {
    val xMiddle = (this.parent as View).width/2
    val yMiddle = (this.parent as View).height/2
    val viewParent = this.parent as View
    val parentHeight = viewParent.height
    val parentWidth = viewParent.width
    val xMax = parentWidth - this.width - this.marginEnd()
    val yMax = parentHeight - this.height - this.marginBottom()

    val marginStart = marginStart()
    val marginTop = marginTop()
    val marginEnd = marginEnd()
    val marginBottom = marginBottom()

    if (currentX >= xMiddle) {

        val rightMarging = resources.getDimensionPixelSize(R.dimen.eight_dp)
        val xMaxWithPadding = Math.min(xMax, xMax - rightMarging)
        if (animated) this.animate().setInterpolator(FastOutLinearInInterpolator()).x(xMaxWithPadding).setDuration(DURATION_MILLIS)
            .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
            }.start()
        else this.x = xMaxWithPadding
    } else {
        val leftMargin = resources.getDimensionPixelSize(R.dimen.eight_dp)
        val maxMarginStart = Math.max(marginStart, marginStart + leftMargin)
        if (animated) this.animate().setInterpolator(FastOutLinearInInterpolator()).x(maxMarginStart).setDuration(DURATION_MILLIS)
            .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
            }.start()
        this.x = maxMarginStart
    }
    val bottomMargin = resources.getDimensionPixelSize(R.dimen.sizeteen_dp)
    val yMaxWithPadding = Math.min(yMax, yMax - bottomMargin)
    if (currentY >= yMiddle) {
        if (animated) this.animate().setInterpolator(FastOutLinearInInterpolator()).y(yMaxWithPadding).setDuration(DURATION_MILLIS)
            .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
            }.start()
        else this.y = yMaxWithPadding
    } else {
        val topMargin = resources.getDimensionPixelSize(R.dimen.sizeteen_dp)
        val maxMarginTop = Math.max(marginTop, marginTop + topMargin)
        if (animated) this.animate().setInterpolator(FastOutLinearInInterpolator()).y(maxMarginTop).setDuration(DURATION_MILLIS)
            .setUpdateListener {
//                                    draggableListener?.onPositionChanged(v)
            }.start()
        else this.y = maxMarginTop
    }

}


val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
