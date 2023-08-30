package live.hms.roomkit.ui.meeting.audiomode

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import live.hms.roomkit.util.dp

class AudioLevelView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    companion object {
        private const val SIDE_BAR_SHRINK_FACTOR = 0.75f
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val barRect = RectF()
    private val barWidth = 4f.dp()
    private val barRadius = 32.dp()
    private val barPadding = 4.dp()
    private var middleBarAnimation: ValueAnimator? = null
    private var sideBarAnimation: ValueAnimator? = null

    private var showAudioLevel = false
    private var lastAudioLevel: Int? = null

    init {
        setWillNotDraw(false)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        update(0)
    }

    private fun mapAudioLevelToScale(level: Int) = when {
        level >= 90 -> 1.0f
        level > 80 -> 0.85f
        level >= 70 -> 0.75f
        level in 50..69 -> 0.65f
        level in 40..49 -> 0.55f
        level in 30..39 -> 0.45f
        level in 10..29 -> 0.35f
        else -> {
            0.15f
        }

    }

    fun update(level: Int?) {

        val wasShowingAudioLevel = showAudioLevel
        showAudioLevel = level != null
        
        if (showAudioLevel) {
            val scaleFactor = mapAudioLevelToScale(level!!)

            middleBarAnimation?.end()

            middleBarAnimation = createAnimation(middleBarAnimation, height * scaleFactor)
            middleBarAnimation?.start()

            sideBarAnimation?.end()

            var finalHeight = height * scaleFactor
            if (level > 10) {
                finalHeight *= SIDE_BAR_SHRINK_FACTOR
            }

            sideBarAnimation = createAnimation(sideBarAnimation, finalHeight)
            sideBarAnimation?.start()
        }

        if (showAudioLevel != wasShowingAudioLevel || level != lastAudioLevel) {
            invalidate()
        }

        lastAudioLevel = level
    }

    private fun createAnimation(current: ValueAnimator?, finalHeight: Float): ValueAnimator {
        val currentHeight = current?.animatedValue as? Float ?: 0f

        return ValueAnimator.ofFloat(currentHeight, finalHeight).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val middleBarHeight = middleBarAnimation?.animatedValue as? Float
        val sideBarHeight = sideBarAnimation?.animatedValue as? Float

       
        if (showAudioLevel && middleBarHeight != null && sideBarHeight != null) {
            val audioLevelWidth = 3 * barWidth + 2 * barPadding
            val xOffsetBase = (width - audioLevelWidth) / 2

            canvas.drawVerticalBar(
                xOffset = xOffsetBase, size = sideBarHeight
            )

            canvas.drawVerticalBar(
                xOffset = barPadding + barWidth + xOffsetBase, size = middleBarHeight
            )

            canvas.drawVerticalBar(
                xOffset = 2 * (barPadding + barWidth) + xOffsetBase, size = sideBarHeight
            )

            if (middleBarAnimation?.isRunning == true || sideBarAnimation?.isRunning == true) {
                invalidate()
            }
        }
    }

    private fun Canvas.drawVerticalBar(xOffset: Float, size: Float) {
        val yOffset = (height - size) / 2
        barRect.set(xOffset, yOffset, xOffset + barWidth, height - yOffset)
        drawRoundRect(barRect, barRadius.toFloat(), barRadius.toFloat(), barPaint)
    }
}