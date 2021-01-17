package live.hms.android100ms.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomDetails(
    val env: String,
    val roomId: String,
    val username: String,
    val authToken: String
) : Parcelable {
    val endpoint = "wss://${env}.100ms.live/ws"
}
