package live.hms.app2.model

import com.google.gson.annotations.SerializedName

data class RecordingInfo(
  @SerializedName("enabled") val enabled: Boolean
)

