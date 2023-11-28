package live.hms.roomkit.ui.meeting.chat.rbac

import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.meeting.chat.Recipient

/**
 * This filter is the one which selects which peer's messages you see
 * When you select a DM vs Everyone.
 * Or a role vs Everyone.
 * If Everyone is selected that means there's no filtering.
 */
class ChatMessageViewFilterHelper {
    private var filterRecipient : Recipient? = null
//    private var filterGroup : String? = null
    fun setFilter(recipient: Recipient?) {
        filterRecipient = recipient
    }
    private fun isSearching() = filterRecipient == Recipient.Everyone
    fun getSearchFilteredPeersIfNeeded(m : List<ChatMessage>) : List<ChatMessage> {

        return when(val filterTargetRecipient = filterRecipient) {
            Recipient.Everyone -> m // no change
            // Always include your own messages
            is Recipient.Peer -> m.filter {
                it.senderPeerId == filterTargetRecipient.peer.peerID || it.isSentByMe
            }
            // Always include your own messages
            is Recipient.Role -> m.filter {
                it.senderRoleName == filterTargetRecipient.role.name || it.isSentByMe
            }
            // Just receive messages even if you can't sent
            null -> m
        }

    }

}