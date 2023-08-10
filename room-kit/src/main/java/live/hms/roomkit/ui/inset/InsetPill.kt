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


class InsetPill : ConstraintLayout {

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

    fun init(context: Context) {


    }


}