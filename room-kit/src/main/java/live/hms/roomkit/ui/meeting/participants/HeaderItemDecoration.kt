package live.hms.roomkit.ui.meeting.participants

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class HeaderItemDecoration(
    @ColorInt background: Int,
    private val sidePaddingPixels: Int,
    @param:LayoutRes private val headerViewType: Int
) : RecyclerView.ItemDecoration() {
    private val paint: Paint

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
                c.drawRect(left, top, right, bottom, paint)
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
        c.drawRect(left, top, right, bottom, paint)
    }
}