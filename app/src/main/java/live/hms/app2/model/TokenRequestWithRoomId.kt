package live.hms.app2.model

import com.google.gson.annotations.SerializedName

data class TokenRequestWithRoomId(
  @SerializedName("room_id") val roomId: String,
  @SerializedName("user_id") val userId: String,
  @SerializedName("role") val role: String,
)

data class TokenRequestWithCode(
  @SerializedName("code") val code: String,
  @SerializedName("user_id") val userId: String,
)
