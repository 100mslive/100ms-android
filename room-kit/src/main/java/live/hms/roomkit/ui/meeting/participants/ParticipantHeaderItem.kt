package live.hms.roomkit.ui.meeting.participants

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ParticipantHeaderItemBinding
import live.hms.roomkit.drawableEnd
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault

class ParticipantHeaderItem(private val roleName: String, private val numPeers: Int? = 0, val expanded : (String, Boolean) -> Unit) :
    BindableItem<ParticipantHeaderItemBinding>(roleName.hashCode().toLong()), ExpandableItem {
    private lateinit var expandableGroup: ExpandableGroup
// heading is onsurface medium
    // background is border default
    override fun bind(viewBinding: ParticipantHeaderItemBinding, position: Int) {
        viewBinding.applyTheme()
        viewBinding.heading.text = viewBinding.root.resources.getString(R.string.participant_header_item, roleName,numPeers)
        viewBinding.root.setOnClickListener {
            expandableGroup.onToggleExpanded()
            expanded(roleName, expandableGroup.isExpanded)
            updateExpandChevron(expandableGroup.isExpanded, viewBinding)
        }
        updateExpandChevron(expandableGroup.isExpanded, viewBinding)
    }

    private fun updateExpandChevron(expanded: Boolean, viewBinding: ParticipantHeaderItemBinding) {
        viewBinding.headerbottom.visibility = if(expanded) View.VISIBLE else View.GONE
        val chevron = if (expanded) R.drawable.chevron_up
        else R.drawable.chevron_down

        viewBinding.heading.drawableEnd = AppCompatResources.getDrawable(
            viewBinding.root.context, chevron
        )?.apply {
            setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
        }
    }

    override fun getLayout(): Int = R.layout.participant_header_item

    override fun initializeViewBinding(view: View): ParticipantHeaderItemBinding =
        ParticipantHeaderItemBinding.bind(view)

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        this.expandableGroup = onToggleListener
    }

}