package live.hms.roomkit.ui.meeting.chat.rbac

import live.hms.video.sdk.models.HMSPeer

/**
 * This filter is for when you're using the search in the messaging options,
 * searching for WHOM you want to message.
 * Like you might be searching for Dmitry to message him.
 * This doesn't do anything yet.
 */
class ChatMessageRecipientTextSearchFilter {
    private var filterText : String? = null
    private var filterGroup : String? = null
    private fun isSearching() = !filterText.isNullOrEmpty()
    private fun getSearchFilteredPeersIfNeeded(peers : List<HMSPeer>) : List<HMSPeer> {
        val text = filterText

        return if (!isSearching())
            peers
        else
            peers.filter {
                text.isNullOrEmpty() || it.name.contains(
                    text.toString(),
                    true
                )
            }
    }
}