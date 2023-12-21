package live.hms.roomkit.ui.polls.leaderboard.item

import android.view.View
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutHeaderBinding
import live.hms.roomkit.databinding.LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding
import live.hms.roomkit.ui.theme.applyTheme

class LeaderBoardHeader(private val titlestr: String, private val subtitle: String = "") :
    BindableItem<LayoutHeaderBinding>() {

    override fun bind(
        viewBinding: LayoutHeaderBinding, position: Int
    ) {
        with(viewBinding) {
            this.title.text = titlestr

            if (subtitle.isEmpty()) this.subheading.visibility = View.GONE
            else this.subheading.visibility = View.VISIBLE
            this.subheading.text = subtitle
            applyTheme()
        }
    }

    override fun getLayout(): Int = R.layout.layout_header


    override fun initializeViewBinding(view: View): LayoutHeaderBinding =
        LayoutHeaderBinding.bind(view)

}