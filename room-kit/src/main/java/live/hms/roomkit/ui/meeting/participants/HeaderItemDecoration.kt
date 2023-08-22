package live.hms.roomkit.ui.meeting.participants

import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class HeaderItemDecoration(
    @ColorInt background: Int,
    private val sidePaddingPixels: Int,
    private val roundedBorderPixels : Float,
    @param:LayoutRes private val headerViewType: Int
) : RecyclerView.ItemDecoration() {
    private val paint: Paint

    private val corners = floatArrayOf(
        roundedBorderPixels, roundedBorderPixels,   // Top left radius in px
        roundedBorderPixels, roundedBorderPixels,   // Top right radius in px
        roundedBorderPixels, roundedBorderPixels,     // Bottom right radius in px
        roundedBorderPixels, roundedBorderPixels      // Bottom left radius in px
    )


    init {
        paint = Paint()
        paint.color = background
        paint.strokeWidth = 1.5f;
        paint.style = Paint.Style.STROKE;
    }

    fun isHeader(child: View?, parent: RecyclerView): Boolean {
        val viewType = parent.layoutManager!!.getItemViewType(child!!)
        return viewType == headerViewType
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (!isHeader(view, parent)) return
        outRect.left = sidePaddingPixels
        outRect.right = sidePaddingPixels
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val lm = parent.layoutManager
        var top : Float = 0f
        var bottom : Float = 0f
        var left : Float = 0f
        var right : Float = 0f

        var foundHeader = false
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val isHeader = isHeader(child, parent)
            // Draw a border from one header to another.
            if(foundHeader && isHeader) {
                val child = parent.getChildAt(i - 1)
                bottom = lm!!.getDecoratedBottom(child) + child.translationY
                foundHeader = false
                drawRoundedBorder(top, bottom, left, right, c, paint)
            }
            if (!isHeader) continue
            foundHeader = true

            top = lm!!.getDecoratedTop(child) + child.translationY

            right = lm.getDecoratedRight(child) + child.translationX
            left = lm.getDecoratedLeft(child) + child.translationX
        }
        // The last header didn't have a header after it so it needs a border drawn to the
        //  final list item. It's fine if that's itself or its child.
        val child = parent.getChildAt(parent.childCount - 1)
        bottom = lm!!.getDecoratedBottom(child) + child.translationY
//        c.drawRect(left, top, right, bottom, paint)
        drawRoundedBorder(top, bottom, left, right, c, paint)
    }

    private fun drawRoundedBorder(
        top: Float,
        bottom: Float,
        left: Float,
        right: Float,
        canvas: Canvas,
        mPaint: Paint
    ) {
        val path = Path()
        path.addRoundRect(RectF(left, top, right, bottom), corners, Path.Direction.CW)
        canvas.drawPath(path, mPaint)
    }
}