package live.hms.roomkit.ui.diagnostic.item

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemDiagnosticDetailBinding
import live.hms.roomkit.horizontalscroll
import live.hms.roomkit.setDrawables
import live.hms.roomkit.ui.theme.applyTheme

class DiagnosticDetail(
    private val title: String,
    private val subTitle: String? = null,
    @DrawableRes private val subtitleIcon: Int
    ) : BindableItem<ItemDiagnosticDetailBinding>() {



    override fun bind(binding: ItemDiagnosticDetailBinding, position: Int) {
        binding.applyTheme()
        binding.header.text = title
        binding.subheader.text = subTitle
        binding.subheader.horizontalscroll()
        binding.subheader.setDrawables(start = binding.subheader.context.resources.getDrawable(subtitleIcon, null))

    }


    override fun getLayout(): Int = R.layout.item_diagnostic_detail
    override fun initializeViewBinding(view: View) = ItemDiagnosticDetailBinding.bind(view)


}