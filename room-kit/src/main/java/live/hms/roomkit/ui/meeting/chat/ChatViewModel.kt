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
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.enums.HMSMessageType
import live.hms.video.sdk.models.role.HMSRole

class ChatViewModel(private val hmssdk: HMSSDK) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }
    private val _currentlySelectedRecipient = MutableLiveData<Recipient?>(null)
    private var lastRecipientNum = 0
    fun setInitialRecipient(recipient: Recipient?,num : Int) {
        if(lastRecipientNum< num) {
            lastRecipientNum = num
            updateSelectedRecipientChatBottomSheet(recipient)
        }
    }

    fun updateSelectedRecipientChatBottomSheet(recipient: Recipient?) {
        // Set a filter for the messages.
        _currentlySelectedRecipient.postValue(recipient)
    }
    val currentlySelectedRecipientRbac : LiveData<Recipient?> = _currentlySelectedRecipient

    private var _messages = mutableListOf<ChatMessage>()

    fun sendMessage(messageStr: String) {
        when (val recipient = currentlySelectedRecipientRbac.value) {
            null -> {} // no-op if it's null
            Recipient.Everyone -> broadcast(
                ChatMessage(
                    senderName = "You",
                    localSenderRealNameForPinMessage = hmssdk.getLocalPeer()?.name ?: DEFAULT_SENDER_NAME,
                    time = null, // Let the server alone set the time
                    message = messageStr,
                    isSentByMe = true,
                    isDmToMe = false,
                    isDm = false,
                    messageId = null,
                    toGroup = ChatMessage.sendTo(
                        recipient = HMSMessageRecipientType.BROADCAST,
                        peer = null,
                        roles = null,
                        false
                    ),
                    senderPeerId = hmssdk.getLocalPeer()?.peerID,
                    senderRoleName = hmssdk.getLocalPeer()?.hmsRole?.name,
                    userIdForBlockList = hmssdk.getLocalPeer()?.customerUserID ?: ""
                )
            )

            is Recipient.Peer -> directMessage(
                ChatMessage(
                    senderName = "You",
                    localSenderRealNameForPinMessage = hmssdk.getLocalPeer()?.name ?: DEFAULT_SENDER_NAME,
                    time = null, // Let the server alone set the time
                    message = messageStr,
                    isSentByMe = true,
                    isDmToMe = false,
                    isDm = true,
                    messageId = null,
                    toGroup = ChatMessage.sendTo(
                        recipient = HMSMessageRecipientType.PEER,
                        peer = recipient.peer,
                        roles = null,
                        sentToMe = false
                    ),
                    senderPeerId = hmssdk.getLocalPeer()?.peerID,
                    senderRoleName = hmssdk.getLocalPeer()?.hmsRole?.name,
                    userIdForBlockList = hmssdk.getLocalPeer()?.customerUserID ?: ""
                ), recipient.peer
            )

            is Recipient.Role -> groupMessage(
                ChatMessage(
                    senderName = "You",
                    localSenderRealNameForPinMessage = hmssdk.getLocalPeer()?.name ?: DEFAULT_SENDER_NAME,
                    time = null, // Let the server alone set the time
                    message = messageStr,
                    isSentByMe = true,
                    isDmToMe = false,
                    isDm = false,
                    messageId = null,
                    toGroup = ChatMessage.sendTo(
                        recipient = HMSMessageRecipientType.ROLES,
                        peer = null,
                        roles = listOf(recipient.role),
                        sentToMe = false
                    ),
                    senderPeerId = hmssdk.getLocalPeer()?.peerID,
                    senderRoleName = hmssdk.getLocalPeer()?.hmsRole?.name,
                    userIdForBlockList = hmssdk.getLocalPeer()?.customerUserID
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
                        addMessage(ChatMessage(hmsMessage, true, false))
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
                        addMessage(ChatMessage(hmsMessage, true, false))
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
                        addMessage(ChatMessage(hmsMessage, true, false))
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
        if(messageIdsToHide?.contains(message.messageId) == true)
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

    private var blockedPeerIds: Set<String> = setOf()

    private fun shouldBlockMessage(message: ChatMessage): Boolean =
        blockedPeerIds.contains(message.userIdForBlockList)

    private var messageIdsToHide : Set<String>? = null
    fun updateMessageHideList(messageIdsToHide: Set<String>) {
        if(messageIdsToHide.isEmpty())
            return
        this.messageIdsToHide = messageIdsToHide
        // Refresh the current list
        _messages = _messages.filter { !messageIdsToHide.contains(it.messageId) }.toMutableList()
        messages.postValue(_messages)
    }

    // The blocklist throws away all messages from the blocked
    //  peer so there's no need to keep running it later.
    //  This is different from a role/peer filter which has to keep the messages.
    fun updateBlockList(chatBlockedPeerIdsList: Set<String>?) {
        val removeBlockedUserMessages = false
        // What does the adapter have to do?
        // Basically turn on a filter.
        // Ok so part of the problem is that we call submit list directly :(
        if (chatBlockedPeerIdsList.isNullOrEmpty()) return
        // Update the blocklist
        blockedPeerIds = chatBlockedPeerIdsList
        if(removeBlockedUserMessages) {
            // Refresh the current list
            _messages =
                _messages.filter { !blockedPeerIds.contains(it.userIdForBlockList) }.toMutableList()
            messages.postValue(_messages)
        }
    }

    fun isUserBlockedFromChat(): Boolean {
        val customerUserId = hmssdk.getLocalPeer()?.customerUserID
        return customerUserId != null && blockedPeerIds.contains(customerUserId)
    }

    fun currentlySelectedRbacRecipient() : Recipient?{
        return _currentlySelectedRecipient.value
    }

    fun updatePeerLeave(leavingPeerId : String?) {
        if(leavingPeerId == null)
            return

        val current = _currentlySelectedRecipient.value
        if(current is Recipient.Peer && current.peer.peerID == leavingPeerId)
            _currentlySelectedRecipient.postValue(null)

    }
}
