package live.hms.app2.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

  companion object {
    private const val TAG = "ChatViewModel"
  }

  private val _messages = ArrayList<String>()
  private var sendBroadcastCallback: ((ChatMessage) -> Unit)? = null

  fun setSendBroadcastCallback(callback: ((message: ChatMessage) -> Unit)) {
    sendBroadcastCallback = callback
  }

  fun removeSendBroadcastCallback() {
    sendBroadcastCallback = null
  }

  val messages = MutableLiveData<ArrayList<String>>()
  val unreadMessagesCount = MutableLiveData(0)

  fun clearMessages() {
    _messages.clear()
    messages.value = _messages
    unreadMessagesCount.postValue(0)
  }

  private fun addMessage(message: String) {
    // Check if the last sender was also the same person
    _messages.add(message)
    messages.value = _messages
  }

  fun receivedMessage(message: String) {
    Log.v(TAG, "receivedMessage: $message")
    unreadMessagesCount.postValue(unreadMessagesCount.value?.plus(1))
    addMessage(message)
  }
}