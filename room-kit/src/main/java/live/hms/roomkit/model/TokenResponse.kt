package live.hms.roomkit.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class TokenResponse(
  @SerializedName("token") val token: String
)