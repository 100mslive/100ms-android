package live.hms.roomkit.ui.meeting.chat

import live.hms.video.sdk.models.HMSMessage
import java.util.*

data class ChatMessage constructor(
    val senderName: String,
    val time: Long? = null,
    val message: String,
    val isSentByMe: Boolean,
    val recipient: Recipient,
    var messageId : String? = null,
) {
    constructor(message: HMSMessage, sentByMe: Boolean) : this(
        message.sender?.name.orEmpty(),
        message.serverReceiveTime,
        message.message,
        sentByMe,
        Recipient.toRecipient(message.recipient),
        messageId = message.messageId
    )
}
