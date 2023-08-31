package live.hms.roomkit.ui.notification

import androidx.annotation.DrawableRes
import live.hms.roomkit.R
import org.w3c.dom.Text

data class HMSNotification(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val isDismissible: Boolean = true,
    val isError: Boolean  = false,
    @DrawableRes val icon: Int = R.drawable.person_icon,
    val type: HMSNotificationType = HMSNotificationType.Default,
    val actionButtonText: String = "Dummy",
)

sealed class HMSNotificationType {
    object ScreenShare : HMSNotificationType()
    object Default : HMSNotificationType()
}