package live.hms.roomkit.model

import com.google.gson.annotations.SerializedName

data class RecordingInfo(
  @SerializedName("enabled") val enabled: Boolean
)

