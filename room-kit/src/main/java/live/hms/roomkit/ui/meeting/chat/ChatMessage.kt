package live.hms.roomkit.ui.meeting.chat

import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.role.HMSRole
import java.util.*

const val DEFAULT_SENDER_NAME = "Participant"
data class ChatMessage constructor(
    val senderName: String,
    val time: Long? = null,
    val message: String,
    val isSentByMe: Boolean,
    val recipient: Recipient,
    var messageId : String? = null,
    val sentTo : String?,
    val toGroup : String?
) {
    companion object {
        fun sendTo(message: HMSMessage) : String? = sendTo(message.recipient.recipientType,
            message.recipient.recipientRoles)

        fun sendTo(recipient: HMSMessageRecipientType,
                           roles : List<HMSRole>?) : String? = when(recipient) {
            HMSMessageRecipientType.BROADCAST -> null
            HMSMessageRecipientType.PEER -> "Direct Message"
            HMSMessageRecipientType.ROLES -> roles?.firstOrNull()?.name ?: "Role"
        }

        fun toGroup(recipient: HMSMessageRecipientType) = when(recipient) {
            HMSMessageRecipientType.PEER, HMSMessageRecipientType.BROADCAST -> null
            HMSMessageRecipientType.ROLES -> "To Group"
        }
    }
    constructor(message: HMSMessage, sentByMe: Boolean) : this(
        if(sentByMe) "You" else message.sender?.name ?: DEFAULT_SENDER_NAME,
        message.serverReceiveTime,
        message.message,
        sentByMe,
        Recipient.toRecipient(message.recipient),
        messageId = message.messageId,
        sentTo = sendTo(message),
        toGroup = toGroup(message.recipient.recipientType)
    )

}
