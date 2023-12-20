package live.hms.roomkit.ui.meeting.chat

import live.hms.video.sdk.models.HMSMessageRecipient
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.role.HMSRole
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed class Recipient {
    object Everyone : Recipient() {
        override fun toString(): String = "Everyone"
    }
    data class Role(val role : HMSRole) : Recipient() {
        override fun toString(): String =
            role.name
    }
    data class Peer(val peer : HMSPeer) : Recipient() {
        override fun toString(): String = peer.name
    }

    companion object {
        fun toRecipient(message : HMSMessageRecipient) : Recipient =
            when(message.recipientType) {
                HMSMessageRecipientType.BROADCAST -> Everyone
                HMSMessageRecipientType.PEER -> Peer(message.recipientPeer!!)
                HMSMessageRecipientType.ROLES -> Role(message.recipientRoles.firstOrNull()!!)

        }
    }
}