package live.hms.roomkit.ui

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemGridOptionBinding
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setBackgroundAndColor

class GridOptionItem(
    private val title: String,
    @DrawableRes val icon: Int,
    private val onClick: () -> Unit,
    var isSelected: Boolean = false,
    var particpantCount: Int? = null,
) : BindableItem<ItemGridOptionBinding>() {

    private val SELECTION_UPDATE = "SELECTION_UPDATE"
    private val UN_SELECTION_UPDATE = "UN_SELECTION_UPDATE"


    override fun bind(viewBinding: ItemGridOptionBinding, position: Int) {
        //themes
        viewBinding.applyTheme()


        viewBinding.subtitle.text = title
        viewBinding.participantImage.setImageResource(icon)

        if (particpantCount != null) {
            viewBinding.badge.visibility = View.VISIBLE
            viewBinding.participantCountText.text = particpantCount.toString()
        } else {
            viewBinding.badge.visibility = View.GONE
        }

        viewBinding.root.setOnClickListener {
            onClick()
        }


    }

    private fun setSelectedView(isSelected: Boolean, v: ItemGridOptionBinding) {
        if (isSelected.not()) v.root.setBackgroundAndColor(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().background_default
        ) else {
            v.root.setBackgroundAndColor(
                HMSPrebuiltTheme.getColours()?.surfaceBrighter,
                HMSPrebuiltTheme.getDefaults().surface_bright
            )
        }

        v.participantImage.setImageResource(icon)
    }

    override fun bind(v: ItemGridOptionBinding, position: Int, payloads: MutableList<Any>) {
        when {
            payloads.contains(SELECTION_UPDATE) -> {
                setSelectedView(isSelected, v)
            }

            else -> bind(v, position)
        }
    }


    override fun getLayout(): Int = R.layout.item_grid_option

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        return spanCount / 3
    }

    fun setSelectedButton(isSelected: Boolean) {
        this.isSelected = isSelected
        notifyChanged(SELECTION_UPDATE)
    }

    override fun initializeViewBinding(view: View) = ItemGridOptionBinding.bind(view)

}