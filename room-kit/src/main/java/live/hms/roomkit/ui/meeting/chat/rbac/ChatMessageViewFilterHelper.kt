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
    private var filterRecipient : Recipient = Recipient.Everyone
//    private var filterGroup : String? = null
    private fun isSearching() = filterRecipient == Recipient.Everyone
    private fun getSearchFilteredPeersIfNeeded(m : List<ChatMessage>) : List<ChatMessage> {
        val filterTargetRecipient = filterRecipient

        return when(filterTargetRecipient) {
            Recipient.Everyone -> m // no change
            is Recipient.Peer -> m.filter {
                it.senderPeerId == filterTargetRecipient.peer.peerID
            }
            is Recipient.Role -> m.filter {
                true // TODO
//                it.roleName == filterTargetRecipient.role.name
            }
        }

    }

}