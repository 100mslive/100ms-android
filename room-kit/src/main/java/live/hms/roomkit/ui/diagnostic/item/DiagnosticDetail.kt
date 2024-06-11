package live.hms.roomkit.ui.diagnostic.item

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemDiagnosticDetailBinding
import live.hms.roomkit.ui.theme.applyTheme

class DiagnosticDetail(
    private val title: String,
    private val subTitle: String? = null,
    @DrawableRes private val subtitleIcon: Int,
    private val isSelected: Boolean,

    ) : BindableItem<ItemDiagnosticDetailBinding>() {

    private lateinit var expand: ExpandableGroup

    override fun bind(binding: ItemDiagnosticDetailBinding, position: Int) {

        binding.root.setOnClickListener {
            expand.onToggleExpanded()
        }

        binding.header.text = title
        binding.subheader.text = subTitle
    }


    override fun getLayout(): Int = R.layout.item_diagnostic_detail
    override fun initializeViewBinding(view: View) = ItemDiagnosticDetailBinding.bind(view)


}