package live.hms.roomkit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomDetails(
  val endPoints : HashMap<String, String>?,
  val url: String,
  val username: String,
  val authToken: String
) : Parcelable
