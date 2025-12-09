package live.hms.roomkit.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import live.hms.roomkit.ui.notification.CallNotificationConfig


@Parcelize
data class HMSPrebuiltOptions(
    val userName: String? = null,
    val userId: String? = null,
    val endPoints: HashMap<String, String>? = null,
    val debugInfo: Boolean = false,
    val callNotificationConfig: CallNotificationConfig? = null,
) : Parcelable