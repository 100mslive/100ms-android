package live.hms.roomkit.ui.meeting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonElement
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.utils.GsonUtils

class HideMessageUseCase: AutoCloseable {
    private lateinit var hmsSessionStore: HmsSessionStore
    val TAG = "HideMesssageUseCase"
    private val HIDE_MESSAGE_KEY = "chatMessageBlacklist"
    val messageIdsToHide: MutableLiveData<Set<String>> = MutableLiveData(setOf())
    private val keyChangeListener = object : HMSKeyChangeListener {
        override fun onKeyChanged(key: String, value: JsonElement?) {
            if (key == HIDE_MESSAGE_KEY) {
                // If the value was null, turn it empty. Only stringify if it isn't.
                val newList: List<String> = if (value == null) {
                    emptyList()
                } else {
                    GsonUtils.gson.fromJson(value.asJsonArray, Array<String>::class.java).toList()
                }
                messageIdsToHide.postValue(newList.toSet())
            }
        }
    }
    fun setSessionStore(hmsSessionStore : HmsSessionStore) {
        this.hmsSessionStore = hmsSessionStore
    }
    fun hideMessage(chatMessage: ChatMessage, resultListener: HMSActionResultListener) {
        if(!::hmsSessionStore.isInitialized ) {
            Log.e(TAG,"Tried to hide message without session store inited")
            return
        }
        if (chatMessage.messageId != null) {
            // the user is no longer present
            // This is a list and will be updated.
            val newValue = messageIdsToHide.value
                // Add the peer id or create a new list if null
                ?.plus(chatMessage.messageId) ?: listOf(chatMessage.messageId)
            hmsSessionStore.set(newValue,
                HIDE_MESSAGE_KEY,
                resultListener)
        }
    }

    fun addKeyChangeListener() {
        hmsSessionStore.addKeyChangeListener(
            listOf(HIDE_MESSAGE_KEY),
            keyChangeListener,
            object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, "Error $error")
                }

                override fun onSuccess() {
                    Log.d(TAG, "Added hide message key")
                }

            })
    }

    override fun close() {
        if(!::hmsSessionStore.isInitialized)
            return
        // remove the key change listeners for this.
        hmsSessionStore.removeKeyChangeListener(
            keyChangeListener,
            object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                }

                override fun onSuccess() {

                }

            })
    }

}