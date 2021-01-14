package live.hms.android100ms.ui.chat

import java.util.*

data class ChatMessage(
    val senderName: String,
    val time: Date,
    val message: String,
) {
    override fun toString(): String {
        return "senderName=${senderName} time=${time} message=${message}"
    }
}