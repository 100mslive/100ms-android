package live.hms.app2.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSCallback
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSPeer

class ChatViewModel(private val hmssdk: HMSSDK) : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<ChatMessage>()
  private val _chatMembers = MutableLiveData<List<Recipient>>(emptyList())
  val chatMembers : LiveData<List<Recipient>> = _chatMembers
  private var currentSelectedRecipient : Recipient = Recipient.Everyone

  fun broadcast(message: ChatMessage) {
    addMessage(message)
    hmssdk.sendBroadcastMessage(message.message, "chat", object : HMSCallback {
      override fun onError(error: HMSException) {
        Log.e(TAG, error.message)
      }

      override fun onSuccess() {
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
      .plus(listOfParticipants.map { Recipient.Role(it.hmsRole.name) }.toSet())
      .plus(listOfParticipants.map { Recipient.Peer(it.peerID, it.name) })
  }

  fun recipientSelected(recipient: Recipient) {
    currentSelectedRecipient = recipient
  }
}