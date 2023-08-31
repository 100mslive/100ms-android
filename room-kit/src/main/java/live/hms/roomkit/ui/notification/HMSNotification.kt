package live.hms.roomkit.ui.notification

data class HMSNotification(val title: String, val isDismissible: Boolean, val isError: Boolean )

sealed class HMSNotificationType {
    object ScreenShare : HMSNotificationType()
}