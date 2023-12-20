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
      //TODO might be able to change the UI to handle send to no one here.
        if(recipient == null)
            return
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