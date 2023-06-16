package live.hms.roomkit.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class HMSPrebuiltOptions(
    val userName: String? = null,
    val userId: String? = null,
    val endPoints: HashMap<String, String>? = null,
    val debugInfo: Boolean = false,
) : Parcelable