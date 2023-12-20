package live.hms.roomkit.ui.meeting.chat.combined

import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.textview.MaterialTextView
import live.hms.roomkit.R
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault

class ChatRbacRecipientHandling {
    fun updateChipRecipientUI(sendToChipText : MaterialTextView,
                              recipient: Recipient?) {
        if(recipient == null) {
            sendToChipText.text =
                sendToChipText.resources.getString(R.string.chat_rbac_picker_send_to)
            // TODO need a search glass image here
            // and to also set the drawable tint
            return
        }
        sendToChipText.text = recipient.toString()
        // Set the drawable next to it
        val chevron = when(recipient) {
            Recipient.Everyone -> R.drawable.tiny_chip_everyone
            is Recipient.Role -> R.drawable.tiny_chip_roles
            is Recipient.Peer -> R.drawable.tiny_chip_dm
        }

        sendToChipText.drawableStart = AppCompatResources.getDrawable(
            sendToChipText.context, chevron
        )?.apply {
            setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                    HMSPrebuiltTheme.getDefaults().onsurface_med_emp
                )
            )
        }
    }
}