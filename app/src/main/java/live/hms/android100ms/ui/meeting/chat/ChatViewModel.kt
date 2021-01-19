package live.hms.android100ms.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<ChatMessageCollection>()
  private var sendBroadcastCallback: ((ChatMessage) -> Unit)? = null

  fun setSendBroadcastCallback(callback: ((message: ChatMessage) -> Unit)) {
    sendBroadcastCallback = callback
  }

  fun removeSendBroadcastCallback() {
    sendBroadcastCallback = null
  }

  private val messages = MutableLiveData<ArrayList<ChatMessageCollection>>()

  fun getMessages(): LiveData<ArrayList<ChatMessageCollection>> = messages
  fun clearMessages() {
    _messages.clear()
    messages.value = _messages
  }

  private fun addMessage(message: ChatMessage) {
    // Check if the last sender was also the same person
    addMessageToChatCollections(_messages, message)
    Log.v(TAG, "addMessage($message) -> ${_messages.size} total collections now")
    messages.value = _messages
  }

  fun broadcast(message: ChatMessage) {
    Log.v(TAG, "broadcastMessage: $message")
    sendBroadcastCallback?.let { it(message) }
    addMessage(message)
  }

  fun receivedMessage(message: ChatMessage) {
    Log.v(TAG, "receivedMessage: $message")
    addMessage(message)
  }
}