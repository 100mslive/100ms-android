package live.hms.roomkit.ui.meeting.chat.rbac

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
const val RECIPIENT_PEERS = "PARTICIPANTS"
const val RECIPIENT_ROLES = "ROLES"
class RecipientHeader(private val recipientHeaderName: String) :
    BindableItem<LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding>(
        recipientHeaderName.hashCode().toLong()
    ), ExpandableItem {
    private lateinit var expandableGroup: ExpandableGroup
    override fun bind(
        viewBinding: LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding,
        position: Int
    ) {
        with(viewBinding) {
            applyTheme()
            name.text = recipientHeaderName
            val headerIcon = when(recipientHeaderName) {
                // everyone is just a recipient item not a header
                RECIPIENT_ROLES -> R.drawable.role_rbac_icon
                RECIPIENT_PEERS -> R.drawable.dm_rbac_icon
                else -> R.drawable.left_arrow
            }
            image.setImageDrawable(
                AppCompatResources.getDrawable(
                viewBinding.root.context, headerIcon
            )?.apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                        HMSPrebuiltTheme.getDefaults().onsurface_med_emp
                    )
                )
            })
            // Don't expand/collapse right now
//            root.setOnSingleClickListener {
//                expandableGroup.onToggleExpanded()
//            }
        }
    }
    override fun getLayout(): Int = R.layout.layout_role_based_chat_message_bottom_sheet_item_header

    override fun initializeViewBinding(view: View): LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding =
        LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding.bind(view)

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        expandableGroup = onToggleListener
    }
}