package live.hms.roomkit.ui.inset

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.contains
import live.hms.roomkit.R


class InsetPill : ConstraintLayout, ScaleGestureDetector.OnScaleGestureListener,
    View.OnTouchListener {

    private lateinit var screenSize: DisplayMetrics
    private val rootView: ViewGroup by lazy { parent as ViewGroup }

    //    private var windowManager: WindowManager? = null
    private var gestureDetector: GestureDetectorCompat? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var scaleFactor = 1.0
    private var popupWidth: Int = 0
    private var popupHeight: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private lateinit var mLayoutParams: FrameLayout.LayoutParams

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context)
    }


    /*
     * Remove layout from window manager
     */
    fun close() {
        if (rootView.contains(this)) rootView.removeView(this)
    }

    fun setGestureDetector(gdc: GestureDetectorCompat) {
        gestureDetector = gdc
    }

    /*
     * Update layout dimensions and apply layout params to window manager
     */
    fun setViewSize(requestedWidth: Int, requestedHeight: Int) {
        var width = requestedWidth
        var height = requestedHeight
        if (width > screenWidth) {
            height = height * screenWidth / width
            width = screenWidth
        }
        if (height > screenHeight) {
            width = width * screenHeight / height
            height = screenHeight
        }
        containInScreen(width, height)
        mLayoutParams.width = width
        mLayoutParams.height = height
        rootView.updateViewLayout(this, mLayoutParams)

    }

    private fun init(context: Context) {


        popupWidth = context.resources.getDimensionPixelSize(R.dimen.inset_pill_width)
        popupHeight = context.resources.getDimensionPixelSize(R.dimen.inset_pill_height)


        val params = WindowManager.LayoutParams(
            popupWidth,
            popupHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )

        params.gravity = Gravity.CENTER
        params.x = 50
        params.y = 50
        scaleGestureDetector = ScaleGestureDetector(context, this)
        setOnTouchListener(this)

        rootView.addView(this, params)


//        updateWindowSize()
    }

//    private fun updateWindowSize() {
//        val size = Point()
//        windowManager!!.defaultDisplay.getSize(size)
//        screenWidth = size.x
//        screenHeight = size.y
//    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (rootView == null) return false
        if (scaleGestureDetector != null) scaleGestureDetector!!.onTouchEvent(event)
        if (gestureDetector != null && gestureDetector!!.onTouchEvent(event)) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                initialX = mLayoutParams.x
//                initialY = mLayoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                Log.d("InsetPill", "ACTION_DOWN")
//                updateWindowSize()
                return true
            }

            MotionEvent.ACTION_UP -> return true
            MotionEvent.ACTION_MOVE -> if (scaleGestureDetector == null || !scaleGestureDetector!!.isInProgress) {
                Log.d("InsetPill", "ACTION_MOVE")
//                mLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
//                mLayoutParams.y = initialY - (event.rawY - initialTouchY).toInt()
                containInScreen(mLayoutParams.width, mLayoutParams.height)
                rootView!!.updateViewLayout(this@InsetPill, mLayoutParams)
                return true
            }
        }
        return false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scaleFactor *= detector.scaleFactor.toDouble()

        scaleFactor = scaleFactor.coerceIn(0.1, 5.0)
        popupWidth = (width * scaleFactor).toInt()
        popupHeight = (height * scaleFactor).toInt()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        setViewSize(popupWidth, popupHeight)
        scaleFactor = 1.0
    }

    private fun containInScreen(width: Int, height: Int) {
//        mLayoutParams.x = mLayoutParams.x.coerceAtLeast(0)
//        mLayoutParams.y = mLayoutParams.y.coerceAtLeast(0)
//        if (mLayoutParams.x + width > screenWidth) mLayoutParams.x = screenWidth - width
//        if (mLayoutParams.y + height > screenHeight) mLayoutParams.y = screenHeight - height
    }

}