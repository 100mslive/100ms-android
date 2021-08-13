package live.hms.app2.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSMessageResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSMessageRecipient
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import live.hms.video.sdk.models.role.HMSRole
import java.util.*
import kotlin.collections.ArrayList

class ChatViewModel(private val hmssdk: HMSSDK) : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<ChatMessage>()
  private val _chatMembers = MutableLiveData<List<Recipient>>(emptyList())
  val chatMembers : LiveData<List<Recipient>> = _chatMembers
  private var currentSelectedRecipient : Recipient = Recipient.Everyone

  fun sendMessage(messageStr : String) {

    val message = ChatMessage(
      "You",
      Date(),
      messageStr,
      true,
      HMSMessageRecipient()
    )

    // Decide where it should go.
    when(val recipient = currentSelectedRecipient) {
      Recipient.Everyone -> broadcast(message.copy(recipient = HMSMessageRecipient()))
      is Recipient.Peer -> directMessage(
        message.copy(recipient = HMSMessageRecipient(recipientPeer = recipient.peer,
                                                  recipientType = HMSMessageRecipientType.PEER)),
        recipient.peer)
      is Recipient.Role -> groupMessage(
        message.copy(recipient = HMSMessageRecipient(recipientRoles = listOf(recipient.role),
          recipientType = HMSMessageRecipientType.ROLES)),
        recipient.role)
    }
  }

  private fun directMessage(message : ChatMessage, peer : HMSPeer) {
    addMessage(message)
    hmssdk.sendDirectMessage(message.message, "chat", peer, object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
      }

    })
  }

  private fun groupMessage(message: ChatMessage, role : HMSRole) {
    addMessage(message)
    hmssdk.sendGroupMessage(message.message, "chat", listOf(role), object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
      }

    })
  }

  private fun broadcast(message: ChatMessage) {
    addMessage(message)
    hmssdk.sendBroadcastMessage(message.message, "chat", object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
      }

    })
  }

  val messages = MutableLiveData<ArrayList<ChatMessage>>()
  val unreadMessagesCount = MutableLiveData(0)

  fun clearMessages() {
    _messages.clear()
    messages.postValue(_messages)
    unreadMessagesCount.postValue(0)
  }

  private fun addMessage(message: ChatMessage) {
    // Check if the last sender was also the same person
    _messages.add(message)
    messages.value = _messages
  }

  fun receivedMessage(message: ChatMessage) {
    Log.v(TAG, "receivedMessage: $message")
    unreadMessagesCount.postValue(unreadMessagesCount.value?.plus(1))
    addMessage(message)
  }

  fun peersUpdate() {
    _chatMembers.postValue(convertPeersToChatMembers(hmssdk.getPeers()))
  }

  private fun convertPeersToChatMembers(listOfParticipants : Array<HMSPeer>) : List<Recipient> {
    return listOf(Recipient.Everyone)
      .plus(listOfParticipants.distinctBy { it.hmsRole.name }.map { Recipient.Role(it.hmsRole) })
      .plus(listOfParticipants.map { Recipient.Peer(it) })
  }

  fun recipientSelected(recipient: Recipient) {
    currentSelectedRecipient = recipient
  }
}