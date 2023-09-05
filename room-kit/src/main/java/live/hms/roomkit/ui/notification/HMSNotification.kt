package live.hms.roomkit.ui.notification

import androidx.annotation.DrawableRes
import live.hms.roomkit.R
import live.hms.video.sdk.models.HMSPeer
import org.w3c.dom.Text

data class HMSNotification(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val isDismissible: Boolean = true,
    val isError: Boolean  = false,
    @DrawableRes val icon: Int = R.drawable.person_icon,
    val type: HMSNotificationType = HMSNotificationType.Default,
    val actionButtonText: String = "",
)

sealed class HMSNotificationType {
    object ScreenShare : HMSNotificationType()
    object Error : HMSNotificationType()
    object TerminalError : HMSNotificationType()
    object Default : HMSNotificationType()
    data class BringOnStage(val handRaisePeer: HMSPeer,val  onStageRole: String) : HMSNotificationType()
}