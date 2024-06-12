package live.hms.roomkit.ui.diagnostic.item

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemDiagnosticDetailBinding
import live.hms.roomkit.databinding.ItemPaddingBinding
import live.hms.roomkit.horizontalscroll
import live.hms.roomkit.setDrawables
import live.hms.roomkit.ui.theme.applyTheme

class Padding(

) : BindableItem<ItemPaddingBinding>() {



    override fun bind(binding: ItemPaddingBinding, position: Int) {


    }


    override fun getLayout(): Int = R.layout.item_padding
    override fun initializeViewBinding(view: View) = ItemPaddingBinding.bind(view)


}