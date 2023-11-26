package live.hms.roomkit.ui.meeting.chat.rbac

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.theme.applyTheme

class RecipientItem(private val recipient: Recipient)
    : BindableItem<LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding>(recipient.hashCode().toLong()) {
    override fun bind(viewBinding: LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding, position: Int) {
        with(viewBinding) {
            applyTheme()
            name.text = getTextForItem(recipient)
        }
    }

    private fun getTextForItem(recipient: Recipient) = when(recipient) {
        Recipient.Everyone -> "Everyone"
        is Recipient.Peer -> recipient.peer.name
        is Recipient.Role -> recipient.role.name
    }

    override fun getLayout(): Int = R.layout.layout_role_based_chat_message_bottom_sheet_item_recipient

    override fun initializeViewBinding(view: View): LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding =
        LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding.bind(view)
}