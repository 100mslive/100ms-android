package live.hms.roomkit.ui.notification

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

/**
 * Configuration for the foreground service notification shown when the app is backgrounded
 * during an active call.
 *
 * Integrating apps can customize the notification to match their branding.
 *
 * Example usage:
 * ```kotlin
 * HMSPrebuiltOptions(
 *     callNotificationConfig = CallNotificationConfig(
 *         smallIcon = R.drawable.my_app_logo,
 *         title = "MyApp - Call Active",
 *         text = "Tap to return to your call"
 *     )
 * )
 * ```
 *
 * @param smallIcon Drawable resource for the small icon shown in status bar and notification header.
 *                  Should be a monochrome icon for best results. If null, uses default icon.
 * @param largeIcon Drawable resource for the large icon shown on the right side of notification.
 *                  Can be a full-color icon. If null, uses default icon.
 * @param title     Notification title. If null, uses "Call in progress".
 * @param text      Notification body text. If null, uses "Tap to return to the call".
 * @param channelName Name for the notification channel. If null, uses "Ongoing Call".
 * @param channelDescription Description for the notification channel. If null, uses default.
 */
@Parcelize
data class CallNotificationConfig(
    @DrawableRes val smallIcon: Int? = null,
    @DrawableRes val largeIcon: Int? = null,
    val title: String? = null,
    val text: String? = null,
    val channelName: String? = null,
    val channelDescription: String? = null
) : Parcelable
