package live.hms.roomkit.ui.meeting.participants

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutViewMoreButtonBinding

class ViewMoreItem(val role : String) : BindableItem<LayoutViewMoreButtonBinding>() {
    override fun bind(viewBinding: LayoutViewMoreButtonBinding, position: Int) {
        // TODO attach onclick listener
    }

    override fun getLayout(): Int = R.layout.layout_view_more_button

    override fun initializeViewBinding(view: View): LayoutViewMoreButtonBinding =
        LayoutViewMoreButtonBinding.bind(view)


}