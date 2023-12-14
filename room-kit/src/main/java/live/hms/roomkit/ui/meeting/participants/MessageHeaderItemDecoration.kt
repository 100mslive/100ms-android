package live.hms.roomkit.ui.meeting.participants

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.util.dp

class MessageHeaderItemDecoration(
    @ColorInt background: Int,
    private val sidePaddingPixels: Int,
    @param:LayoutRes private val headerViewType: Int
) :
    RecyclerView.ItemDecoration() {
    private val paint: Paint

    init {
        paint = Paint()
        paint.color = background
        paint.strokeWidth = 1f.dp()
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
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (!isHeader(child, parent)) continue
            val lm = parent.layoutManager
            val top = lm!!.getDecoratedTop(child) + child.translationY
//            var bottom = lm.getDecoratedBottom(child) + child.translationY
//            if (i == parent.childCount - 1) {
//                // Draw to bottom if last item
//                bottom = Math.max(parent.height.toFloat(), bottom)
//            }
            val right = lm.getDecoratedRight(child) + child.translationX
            val left = lm.getDecoratedLeft(child) + child.translationX
            c.drawLine(left, top, right, top, paint)
        }
    }
}