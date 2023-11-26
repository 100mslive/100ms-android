package live.hms.roomkit.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSMessageResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.enums.HMSMessageType
import live.hms.video.sdk.models.role.HMSRole

data class SelectedRecipient(
    val recipients: List<Recipient>, val index: Int
)

class ChatViewModel(private val hmssdk: HMSSDK) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }
    private val _currentlySelectedRecipient = MutableLiveData<Recipient>(Recipient.Everyone)
    fun updateSelectedRecipientChatBottomSheet(recipient: Recipient) {
        _currentlySelectedRecipient.postValue(recipient)
    }
    val currentlySelectedRecipientRbac : LiveData<Recipient> = _currentlySelectedRecipient

    private var _messages = mutableListOf<ChatMessage>()

    fun sendMessage(messageStr: String) {

        // Decide where it should go.
        when (val recipient = currentlySelectedRecipientRbac.value!!) {
            Recipient.Everyone -> broadcast(
                ChatMessage(
                    "You",
                    null, // Let the server alone set the time
                    messageStr,
                    true,
                    null,
                    ChatMessage.sendTo(HMSMessageRecipientType.BROADCAST, null),
                    ChatMessage.toGroup(HMSMessageRecipientType.BROADCAST),
                    hmssdk.getLocalPeer()?.peerID
                )
            )

            is Recipient.Peer -> directMessage(
                ChatMessage(
                    "You",
                    null, // Let the server alone set the time
                    messageStr,
                    true,

                    null,
                    ChatMessage.sendTo(HMSMessageRecipientType.PEER, null),
                    ChatMessage.toGroup(HMSMessageRecipientType.PEER),
                    hmssdk.getLocalPeer()?.peerID
                ), recipient.peer
            )

            is Recipient.Role -> groupMessage(
                ChatMessage(
                    "You",
                    null, // Let the server alone set the time
                    messageStr,
                    true,
                    null,
                    ChatMessage.sendTo(HMSMessageRecipientType.ROLES, listOf(recipient.role)),
                    ChatMessage.toGroup(HMSMessageRecipientType.ROLES),
                    hmssdk.getLocalPeer()?.peerID!!
                ), recipient.role
            )
        }
    }

    private fun directMessage(message: ChatMessage, peer: HMSPeer) {

        hmssdk.sendDirectMessage(message.message,
            HMSMessageType.CHAT,
            peer,
            object : HMSMessageResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, error.message)
                }

                override fun onSuccess(hmsMessage: HMSMessage) {
                    // Request Successfully sent to server
                    MainScope().launch {
                        addMessage(ChatMessage(hmsMessage, true))
                    }
                }

            })
    }

    private fun groupMessage(message: ChatMessage, role: HMSRole) {

        hmssdk.sendGroupMessage(message.message,
            HMSMessageType.CHAT,
            listOf(role),
            object : HMSMessageResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, error.message)
                }

                override fun onSuccess(hmsMessage: HMSMessage) {
                    // Request Successfully sent to server
                    MainScope().launch {
                        addMessage(ChatMessage(hmsMessage, true))
                    }
                }

            })
    }

    private fun broadcast(message: ChatMessage) {

        hmssdk.sendBroadcastMessage(message.message,
            HMSMessageType.CHAT,
            object : HMSMessageResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, error.message)
                }

                override fun onSuccess(hmsMessage: HMSMessage) {
                    // Request Successfully sent to server
                    MainScope().launch {
                        addMessage(ChatMessage(hmsMessage, true))
                    }
                }

            })
    }

    // This contains all messages so when we switch between views we don't lose them.
    //  otherwise the chat would only have messages that were received when it opened.
    //  and lose all of them when it was closed.
    val messages = MutableLiveData<List<ChatMessage>>()
    val unreadMessagesCount = MutableLiveData(0)

    fun clearMessages() {
        _messages.clear()
        messages.postValue(_messages)
        unreadMessagesCount.postValue(0)
    }

    private fun addMessage(message: ChatMessage) {
        // Check if the last sender was also the same person
        if(shouldBlockMessage(message))
            return

        if (_messages.find { it.messageId == message.messageId } == null) {
            if (!message.isSentByMe) {
                unreadMessagesCount.postValue(unreadMessagesCount.value?.plus(1))
            }
            _messages.add(message)
            messages.postValue(_messages)
        }
    }

    fun receivedMessage(message: ChatMessage) {
        Log.v(TAG, "receivedMessage: $message")
        MainScope().launch {
            addMessage(message)
        }
    }

    private fun convertPeersToChatMembers(
        listOfParticipants: List<HMSRemotePeer>, roles: List<HMSRole>
    ): List<Recipient> {
        return listOf(Recipient.Everyone).plus(roles.map { Recipient.Role(it) })
            // Remove local peers (yourself) from the list of people you can message.
            .plus(listOfParticipants.map { Recipient.Peer(it) })
    }

    private var blockedPeerIds: Set<String> = setOf()

    private fun shouldBlockMessage(message: ChatMessage): Boolean =
        blockedPeerIds.contains(message.senderPeerId)


    fun updateBlockList(chatBlockedPeerIdsList: List<String>?) {
        // What does the adapter have to do?
        // Basically turn on a filter.
        // Ok so part of the problem is that we call submit list directly :(
        if (chatBlockedPeerIdsList == null) return
        blockedPeerIds = chatBlockedPeerIdsList.toSet()
        // Refresh the current list
        _messages = _messages.filter { !blockedPeerIds.contains(it.senderPeerId) }.toMutableList()
        messages.postValue(_messages)
    }

    fun isUserBlockedFromChat(): Boolean {
        val peerId = hmssdk.getLocalPeer()?.peerID
        return peerId != null && blockedPeerIds.contains(peerId)
    }
}
