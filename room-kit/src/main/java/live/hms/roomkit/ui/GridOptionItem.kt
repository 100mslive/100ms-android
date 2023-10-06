package live.hms.roomkit.ui

import android.view.View
import androidx.annotation.DrawableRes
import com.google.android.material.shape.CornerFamily
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemGridOptionBinding
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape
import live.hms.roomkit.ui.theme.setBackgroundAndColor

class GridOptionItem(
    private var title: String,
    @DrawableRes val icon: Int,
    private val onClick: () -> Unit,
    var isSelected: Boolean = false,
    var particpantCount: Int? = null,
    var showProgress: Boolean = false,
) : BindableItem<ItemGridOptionBinding>() {

    private val SELECTION_UPDATE = "SELECTION_UPDATE"
    private val TEXT_UPDATE = "TEXT_UPDATE"
    private val PARTICPANT_COUNt_UPDATE = "PARTICPANT_COUNt_UPDATE"
    private val PROGRESS_UPDATE = "PROGRESS_UPDATE"


    override fun bind(viewBinding: ItemGridOptionBinding, position: Int) {
        //themes
        viewBinding.participantImage.setImageResource(icon)
        viewBinding.applyTheme()



        viewBinding.subtitle.text = title


        if (particpantCount != null) {

            viewBinding.participantCountText.visibility = View.VISIBLE
            viewBinding.participantCountText.text = particpantCount.toString()
        } else {
            viewBinding.participantCountText.visibility = View.GONE
        }

        viewBinding.root.setOnClickListener {
            if (showProgress.not())
            onClick()
        }

        if (showProgress) {
            viewBinding.progressBar.visibility = View.VISIBLE
            viewBinding.nonpregressGroup.visibility = View.INVISIBLE
        } else {
            viewBinding.progressBar.visibility = View.GONE
            viewBinding.nonpregressGroup.visibility = View.VISIBLE
        }

        setSelectedView(isSelected, viewBinding)
    }

    private fun setSelectedView(isSelected: Boolean, v: ItemGridOptionBinding) {
        v.rootLayout.background = if (isSelected.not()) {
             getShape().apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.backgroundDefault,
                        HMSPrebuiltTheme.getDefaults().background_default
                    )
                )
            }
        }
         else {
            getShape().apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.surfaceBrighter,
                        HMSPrebuiltTheme.getDefaults().surface_bright,
                    )
                )
            }
        }
    }

    override fun bind(v: ItemGridOptionBinding, position: Int, payloads: MutableList<Any>) {

        if (payloads.contains(SELECTION_UPDATE)) {
            setSelectedView(isSelected, v)
        }
        if (payloads.contains(PARTICPANT_COUNt_UPDATE)) {
            if (particpantCount != null) {
                v.participantCountText.visibility = View.VISIBLE
                v.participantCountText.text = particpantCount.toString()
            } else {
                v.participantCountText.visibility = View.GONE
            }
        }
        if (payloads.contains(TEXT_UPDATE)) {
            v.subtitle.text = title
        }

        if (payloads.contains(PROGRESS_UPDATE)) {
            if (showProgress) {
                v.progressBar.visibility = View.VISIBLE
                v.nonpregressGroup.visibility = View.INVISIBLE
            } else {
                v.progressBar.visibility = View.GONE
                v.nonpregressGroup.visibility = View.VISIBLE
            }
        }

        bind(v, position)

    }


    override fun getLayout(): Int = R.layout.item_grid_option

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        return spanCount / 3
    }

    fun setSelectedButton(isSelected: Boolean) {
        this.isSelected = isSelected
        notifyChanged(SELECTION_UPDATE)
    }

    fun setText(text: String) {
        this.title = text
        notifyChanged(TEXT_UPDATE)
    }

    fun setParticpantCountUpdate(count: Int?) {
        this.particpantCount = count
        notifyChanged(PARTICPANT_COUNt_UPDATE)
    }

    fun showProgress(enable: Boolean) {
        this.showProgress = enable
        notifyChanged(PROGRESS_UPDATE)
    }

    override fun initializeViewBinding(view: View) = ItemGridOptionBinding.bind(view)

}