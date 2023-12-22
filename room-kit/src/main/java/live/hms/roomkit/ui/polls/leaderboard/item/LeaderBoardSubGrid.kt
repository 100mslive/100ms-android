package live.hms.roomkit.ui.polls.leaderboard.item

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemGridOptionBinding
import live.hms.roomkit.databinding.ItemGridSubTextBinding
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape

class LeaderBoardSubGrid(
    private var title: String, private var subtitle: String
) : BindableItem<ItemGridSubTextBinding>() {


    override fun bind(viewBinding: ItemGridSubTextBinding, position: Int) {
        //themes
        viewBinding.applyTheme()
        viewBinding.subtitle.text = subtitle
        viewBinding.heading.text = title


    }


    override fun getLayout(): Int = R.layout.item_grid_sub_text

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        return spanCount / 2
    }

    override fun initializeViewBinding(view: View) = ItemGridSubTextBinding.bind(view)

}