package live.hms.roomkit.ui.meeting.chat.rbac

import android.view.View
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.theme.applyTheme

class RecipientHeader(private val recipientHeaderName: String) :
    BindableItem<LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding>(
        recipientHeaderName.hashCode().toLong()
    ), ExpandableItem {
    override fun bind(
        viewBinding: LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding,
        position: Int
    ) {
        with(viewBinding) {
            applyTheme()
            name.text = recipientHeaderName
        }
    }
    override fun getLayout(): Int = R.layout.layout_role_based_chat_message_bottom_sheet_item_header

    override fun initializeViewBinding(view: View): LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding =
        LayoutRoleBasedChatMessageBottomSheetItemHeaderBinding.bind(view)

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
//        TODO("Not yet implemented")
    }
}