package live.hms.app2.ui.meeting.chat

import java.util.*

data class ChatMessage(
  val senderName: String,
  val time: Date,
  val message: String,
  val isSentByMe: Boolean,
  val recipient: Recipient
)

