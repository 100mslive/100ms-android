package live.hms.roomkit.ui.meeting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.utils.GsonUtils

class PauseChatUseCase : AutoCloseable {
    private lateinit var hmsSessionStore: HmsSessionStore
    val TAG = "BlockUserUseCase"
    private val PAUSE_CHAT_KEY = "chatState"
    val currentChatPauseState: MutableLiveData<ChatPauseState> = MutableLiveData(ChatPauseState())
    private val keyChangeListener = object : HMSKeyChangeListener {
        override fun onKeyChanged(key: String, value: JsonElement?) {
            if (key == PAUSE_CHAT_KEY) {
                // If the value was null, turn it empty. Only stringify if it isn't.
                val newState: ChatPauseState = if (value == null) {
                    ChatPauseState()
                } else {
                    GsonUtils.gson.fromJson(value, ChatPauseState::class.java)
                }
                currentChatPauseState.postValue(newState)
            }
        }
    }

    fun setSessionStore(hmsSessionStore: HmsSessionStore) {
        this.hmsSessionStore = hmsSessionStore
    }

    fun changeChatState(state: ChatPauseState) {
        if (!::hmsSessionStore.isInitialized) {
            Log.e(TAG, "Tried to block user without session store inited")
            return
        }

        hmsSessionStore.set(state,
            PAUSE_CHAT_KEY,
            object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    Log.e(TAG, "Updating chat state failed with $error")
                }

                override fun onSuccess() {
                    Log.d(TAG, "Updating chat state successful")
                }

            })

    }

    fun addKeyChangeListener() {
        hmsSessionStore.addKeyChangeListener(
            listOf(PAUSE_CHAT_KEY),
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
        if (!::hmsSessionStore.isInitialized)
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

data class ChatPauseState(
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("updatedBy")
    val updatedBy: UpdatedBy = UpdatedBy(),
    @SerializedName("updatedAt")
    val updatedAt : Long = System.currentTimeMillis()
) {
    data class UpdatedBy(
        @SerializedName("peerID")
        val peerID: String = "",
        @SerializedName("userID")
        val userID : String = "",
        @SerializedName("userName")
        val userName : String = "",
    )
}