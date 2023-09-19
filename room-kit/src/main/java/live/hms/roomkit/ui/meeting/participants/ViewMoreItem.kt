package live.hms.roomkit.ui.meeting.participants

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutViewMoreButtonBinding

class ViewMoreItem(val role : String, private val onClick:(role: String) -> Unit) : BindableItem<LayoutViewMoreButtonBinding>() {
    override fun bind(viewBinding: LayoutViewMoreButtonBinding, position: Int) {
        // TODO attach onclick listener
        viewBinding.viewMore.setOnClickListener {
            onClick(role)
        }
    }

    override fun getLayout(): Int = R.layout.layout_view_more_button

    override fun initializeViewBinding(view: View): LayoutViewMoreButtonBinding =
        LayoutViewMoreButtonBinding.bind(view)


}