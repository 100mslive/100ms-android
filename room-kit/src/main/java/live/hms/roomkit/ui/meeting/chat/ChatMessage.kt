package live.hms.roomkit.ui.meeting.chat

import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.role.HMSRole
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import live.hms.roomkit.ui.meeting.chat.rbac.RecipientItem
import live.hms.video.sdk.models.HMSMessageRecipient
import live.hms.video.sdk.models.HMSPeer

const val DEFAULT_SENDER_NAME = "Participant"


@Parcelize
data class ChatMessage constructor(
    val senderName: String,
    val localSenderRealNameForPinMessage : String,
    val time: Long? = null,
    val message: String,
    val isSentByMe: Boolean,
    val isDmToMe : Boolean,
    val isDm : Boolean,
    var messageId : String? = null,
    val toGroup : String?,
    val senderPeerId : String?,
    val senderRoleName : String?,
    val userIdForBlockList : String?,
)  : Parcelable {
    companion object {
        fun sendTo(recipient: HMSMessageRecipient, sentByMe: Boolean, sentToMe: Boolean) : String? =
            sendTo(
                recipient.recipientType,
                recipient.recipientPeer,
                recipient.recipientRoles,
                sentToMe
            )

        fun sendTo(
            recipient: HMSMessageRecipientType,
            peer: HMSPeer?,
            roles: List<HMSRole>?,
            sentToMe: Boolean
        ) : String? = when(recipient) {
            HMSMessageRecipientType.BROADCAST -> null
            HMSMessageRecipientType.PEER -> if(sentToMe) "You" else peer?.name // We don't know if this should be 'you' or 'name'
            HMSMessageRecipientType.ROLES -> roles?.firstOrNull()?.name ?: "Role"
        }
    }
    constructor(message: HMSMessage, sentByMe: Boolean, sentToMe : Boolean) : this(
        if(sentByMe) "You" else message.sender?.name ?: DEFAULT_SENDER_NAME,
        message.sender?.name ?: DEFAULT_SENDER_NAME,
        message.serverReceiveTime,
        message.message,
        sentByMe,
        sentToMe,
        message.recipient.recipientType == HMSMessageRecipientType.PEER,
        messageId = message.messageId,
        toGroup = sendTo(message.recipient, sentByMe, sentToMe),
        message.sender?.peerID,
        message.sender?.hmsRole?.name,
        message.sender?.customerUserID
    )

}
