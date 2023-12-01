package live.hms.roomkit.ui.meeting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonElement
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.utils.GsonUtils.gson

class BlockUserUseCase: AutoCloseable {
    private lateinit var hmsSessionStore: HmsSessionStore
    val TAG = "BlockUserUseCase"
    private val BLOCK_PEER_KEY = "chatPeerBlacklist"
    val currentBlockList: MutableLiveData<Set<String>> = MutableLiveData(emptySet())
    private val keyChangeListener = object : HMSKeyChangeListener {
        override fun onKeyChanged(key: String, value: JsonElement?) {
            if (key == BLOCK_PEER_KEY) {
                // If the value was null, turn it empty. Only stringify if it isn't.
                val newList: Set<String> = if (value == null) {
                    setOf()
                } else {
                    gson.fromJson(value.asJsonArray, Array<String>::class.java).toSet()
                }
                currentBlockList.postValue(newList)
            }
        }
    }
    fun setSessionStore(hmsSessionStore : HmsSessionStore) {
        this.hmsSessionStore = hmsSessionStore
    }
    fun blockUser(chatMessage: ChatMessage, hmsActionResultListener: HMSActionResultListener) {
        if(!::hmsSessionStore.isInitialized ) {
            Log.e(TAG,"Tried to block user without session store inited")
            return
        }
        if (chatMessage.userIdForBlockList != null) {
            // the user is no longer present
            // This is a list and will be updated.
            val newValue = currentBlockList.value
                // Add the peer id or create a new list if null
                ?.plus(chatMessage.userIdForBlockList) ?: setOf(chatMessage.userIdForBlockList)
            hmsSessionStore.set(newValue,
                BLOCK_PEER_KEY,hmsActionResultListener)
        }
    }

    fun addKeyChangeListener() {
        hmsSessionStore.addKeyChangeListener(
            listOf(BLOCK_PEER_KEY),
            keyChangeListener,
            object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, "Error $error")
                }

                override fun onSuccess() {
                    Log.d(TAG, "Added block peer key")
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