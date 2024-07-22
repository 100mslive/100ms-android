package live.hms.roomkit.ui.meeting.chat

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import live.hms.prebuilt_themes.dp
import live.hms.roomkit.R


class SingleSideFadeRecyclerview  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.recyclerview.R.attr.recyclerViewStyle
) : RecyclerView(context, attrs, defStyleAttr) {
    var isHeightContrained = false

    // Don't fade the bottom, only the top
    override fun getBottomFadingEdgeStrength(): Float = 0f
    override fun getTopFadingEdgeStrength(): Float {
        return super.getTopFadingEdgeStrength()*2
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(isHeightContrained) {
            var newSpec = heightMeasureSpec
            newSpec = MeasureSpec.makeMeasureSpec(300.dp(), MeasureSpec.AT_MOST)
            super.onMeasure(widthMeasureSpec, newSpec)
        } else super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    init {

        layoutManager = LinearLayoutManager(context)
            .apply {
                stackFromEnd = true
            }
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(140)
        val divider = DividerItemDecoration(context, VERTICAL).apply {
            setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.chat_item_divider, null)!!)
        }
        addItemDecoration(divider)
    }
}