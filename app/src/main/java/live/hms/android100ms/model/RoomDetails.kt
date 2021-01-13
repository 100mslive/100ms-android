package live.hms.android100ms.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomDetails(
    val endpoint: String,
    val roomId: String,
    val username: String,
    val authToken: String
) : Parcelable {
    val env = endpoint.split(".")[0].replace("wss://", "");
}
