package live.hms.app2.ui.meeting.participants

import android.content.Context
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes

class RtmpAdapter(
    context: Context,
    @LayoutRes layout: Int,
    items: List<String>
) : ArrayAdapter<String>(context, layout, items) {

}