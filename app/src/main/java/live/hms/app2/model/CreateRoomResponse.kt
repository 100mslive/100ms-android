package live.hms.app2.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class CreateRoomResponse(
  @SerializedName("active") val active: Boolean,
  @SerializedName("created_at") val createdAt: Date,
  @SerializedName("customer") val customer: String,
  @SerializedName("description") val description: String,
  @SerializedName("id") val roomId: String,
  @SerializedName("name") val name: String,
  @SerializedName("recording_info") val recordingInfo: RecordingInfo,
  @SerializedName("updated_at") val updatedAt: Date,
  @SerializedName("user") val user: String
)
