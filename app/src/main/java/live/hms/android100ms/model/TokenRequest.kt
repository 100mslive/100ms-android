package live.hms.android100ms.model

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("room_id") val roomId: String,
    @SerializedName("user_name") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("env") val environment: String
)