package live.hms.android100ms.ui.meeting.chat

import java.util.*

data class ChatMessage(
  val senderName: String,
  val time: Date,
  val message: String,
  val isSentByMe: Boolean,
) {
  override fun toString(): String {
    return "ChatMessage(senderName=${senderName} time=${time} message='${message}', isSentByMe=${isSentByMe})"
  }
}