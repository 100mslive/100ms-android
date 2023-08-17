package live.hms.roomkit.ui.meeting.activespeaker

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SingleSideFadeRecyclerview  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.recyclerview.R.attr.recyclerViewStyle
) : RecyclerView(context, attrs, defStyleAttr) {

    // Don't fade the bottom, only the top
    override fun getBottomFadingEdgeStrength(): Float = 0f
//    override fun getTopFadingEdgeStrength(): Float {
//        return super.getTopFadingEdgeStrength()*2
//    }

}