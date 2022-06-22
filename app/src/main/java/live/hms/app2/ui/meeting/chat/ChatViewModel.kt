package live.hms.app2.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSMessageResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer
import live.hms.video.sdk.models.enums.HMSMessageType
import live.hms.video.sdk.models.role.HMSRole
import java.util.*
import kotlin.collections.ArrayList

data class SelectedRecipient(val recipients: List<Recipient>,
                             val index : Int)

class ChatViewModel(private val hmssdk: HMSSDK) : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<ChatMessage>()
  private val _chatMembers = MutableLiveData<SelectedRecipient>()
  val chatMembers : LiveData<SelectedRecipient> = _chatMembers
  private var currentSelectedRecipient : Recipient = Recipient.Everyone

  fun sendMessage(messageStr : String) {

    val message = ChatMessage(
      "You",
      System.currentTimeMillis(),
      messageStr,
      true,
      Recipient.Everyone
    )

    // Decide where it should go.
    when(val recipient = currentSelectedRecipient) {
      Recipient.Everyone -> broadcast( message )
      is Recipient.Peer -> directMessage(
        message.copy(recipient = Recipient.Peer(recipient.peer)),
        recipient.peer)
      is Recipient.Role -> groupMessage(
        message.copy(recipient = Recipient.Role(recipient.role)),
        recipient.role)
    }
  }

  private fun directMessage(message : ChatMessage, peer : HMSPeer) {

    hmssdk.sendDirectMessage(message.message, HMSMessageType.CHAT, peer, object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
        addMessage(ChatMessage(hmsMessage, true))
      }

    })
  }

  private fun groupMessage(message: ChatMessage, role : HMSRole) {

    hmssdk.sendGroupMessage(message.message, HMSMessageType.CHAT, listOf(role), object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
        addMessage(ChatMessage(hmsMessage, true))
      }

    })
  }

  private fun broadcast(message: ChatMessage) {

    hmssdk.sendBroadcastMessage(message.message, HMSMessageType.CHAT, object : HMSMessageResultListener {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess(hmsMessage: HMSMessage) {
        // Request Successfully sent to server
        addMessage(ChatMessage(hmsMessage, true))
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
    messages.postValue(_messages)
  }

  fun receivedMessage(message: ChatMessage) {
    Log.v(TAG, "receivedMessage: $message")
    unreadMessagesCount.postValue(unreadMessagesCount.value?.plus(1))
    addMessage(message)
  }

  fun peersUpdate() {
    val list = convertPeersToChatMembers(hmssdk.getRemotePeers().toList(), hmssdk.getRoles())
    val currentIndex = when(val num = list.indexOf(currentSelectedRecipient)) {
      -1 -> 0
      else -> num
    }
    _chatMembers.postValue(SelectedRecipient(list, currentIndex))
  }

  private fun convertPeersToChatMembers(listOfParticipants : List<HMSRemotePeer>, roles : List<HMSRole>) : List<Recipient> {
    return listOf(Recipient.Everyone)
      .plus(roles.map { Recipient.Role(it) })
      // Remove local peers (yourself) from the list of people you can message.
      .plus(listOfParticipants.map { Recipient.Peer(it) })
  }

  fun recipientSelected(recipient: Recipient) {
    currentSelectedRecipient = recipient
  }

  init {
    peersUpdate() // Load up local peers into the chat members.
  }
}