package live.hms.android100ms.ui.chat

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val messagesList = ArrayList<ChatMessage>()

    val broadcastMessage = MutableLiveData<ChatMessage>()
    val receivedMessage = MutableLiveData<ChatMessage>()

    fun getAllMessages() = messagesList

    fun broadcast(message: ChatMessage) {
        Log.v(TAG, "broadcastMessage: $message")
        messagesList.add(message)
        broadcastMessage.value = message
        // broadcastMessage.postValue(message)
    }

    fun receivedMessage(message: ChatMessage) {
        Log.v(TAG, "receivedMessage: $message")
        messagesList.add(message)
        receivedMessage.value = message
    }
}