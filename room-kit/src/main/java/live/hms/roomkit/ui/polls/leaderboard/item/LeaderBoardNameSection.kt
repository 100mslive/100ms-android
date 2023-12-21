package live.hms.roomkit.ui.polls.leaderboard.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemNameSectionBinding
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape

class LeaderBoardNameSection(
    private val titleStr: String,
    private val subtitleStr: String,
    private val rankStr: Int,
    private val timetakenStr: String? = null,
    private val correctAnswerStr: String? = null,
    private val isSelected: Boolean
) : BindableItem<ItemNameSectionBinding>() {


    override fun bind(viewBinding: ItemNameSectionBinding, position: Int) {
        //themes
        setSelectedView(isSelected, viewBinding)

        with(viewBinding) {
            applyTheme()
            heading.text = titleStr
            subtitle.text = subtitleStr
            rank.text = rankStr.toString()

            if (timetakenStr.isNullOrEmpty().not()) {
                timeTaken.visibility = View.VISIBLE
                timeTaken.text = timetakenStr.toString()
            } else {
                timeTaken.visibility = View.GONE
            }

            if (correctAnswerStr.isNullOrEmpty().not()) {
                correctAnswer.visibility = View.VISIBLE
                correctAnswer.text = correctAnswerStr.toString()
            } else {
                correctAnswer.visibility = View.GONE
            }

        }
    }

    private fun setSelectedView(isSelected: Boolean, v: ItemNameSectionBinding) {
        v.rootLayout.background = if (isSelected.not()) {
            getShape().apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.backgroundDefault,
                        HMSPrebuiltTheme.getDefaults().background_default
                    )
                )
            }
        } else {
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


    override fun getLayout(): Int = R.layout.item_name_section


    override fun initializeViewBinding(view: View) = ItemNameSectionBinding.bind(view)

}