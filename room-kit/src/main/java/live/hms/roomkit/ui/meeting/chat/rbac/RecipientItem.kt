package live.hms.roomkit.ui.meeting.chat.rbac

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault

class RecipientItem(private val recipient: Recipient,
                    private val currentSelectedRecipient: Recipient?,
                    private val onItemClicked : (Recipient) -> Unit)
    : BindableItem<LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding>(recipient.hashCode().toLong()) {
    override fun bind(viewBinding: LayoutRoleBasedChatMessageBottomSheetItemRecipientBinding, position: Int) {
        with(viewBinding) {
            applyTheme()
            root.setOnSingleClickListener {
                onItemClicked(recipient)
            }
            name.text = getTextForItem(recipient)
            tick.visibility = if(currentSelectedRecipient == recipient) View.VISIBLE else View.GONE
            image.visibility = if(recipient == Recipient.Everyone) View.VISIBLE else View.GONE
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