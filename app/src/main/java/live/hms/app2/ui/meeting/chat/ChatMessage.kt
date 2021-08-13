package live.hms.app2.ui.meeting.chat

import live.hms.video.sdk.models.HMSMessageRecipient
import java.util.*

data class ChatMessage(
  val senderName: String,
  val time: Date,
  val message: String,
  val isSentByMe: Boolean,
  val recipient: HMSMessageRecipient
)

