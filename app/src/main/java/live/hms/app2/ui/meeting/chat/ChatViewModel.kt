package live.hms.app2.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<ChatMessage>()
  private var sendBroadcastCallback: ((String) -> Unit)? = null

  fun broadcast(message: ChatMessage) {
    addMessage(message)
    sendBroadcastCallback?.invoke(message.message)
  }

  fun setSendBroadcastCallback(callback: ((message: String) -> Unit)) {
    sendBroadcastCallback = callback
  }

  fun removeSendBroadcastCallback() {
    sendBroadcastCallback = null
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
}