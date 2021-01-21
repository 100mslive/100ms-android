package live.hms.android100ms.ui.meeting.chat

import java.util.*

data class ChatMessage(
  val customerId: String,
  val senderName: String,
  val time: Date,
  val message: String,
  val isSentByMe: Boolean,
)

