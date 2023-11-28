package live.hms.roomkit.ui.meeting

import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.utils.GsonUtils
import java.io.Closeable

private const val PINNED_MESSAGE_SESSION_KEY: String = "pinnedMessages"
class SessionMetadataUseCase : Closeable {
    private val MAX_PINNED_MESSAGES = 3

    private lateinit var hmsSessionStore: HmsSessionStore
    private var localPeerName : String? = null
    private val keyListener = object : HMSKeyChangeListener {
        override fun onKeyChanged(key: String, value: JsonElement?) {
            if(key == PINNED_MESSAGE_SESSION_KEY) {
                // If the value was null, leave it null. Only stringify if it isn't.
                val message = if (value == null) {
                    emptyArray()
                } else {
                    GsonUtils.gson.fromJson(value.asJsonArray, Array<PinnedMessage>::class.java)
                }

                pinnedMessages.postValue(message)
            }
        }
    }
    val pinnedMessages : MutableLiveData<Array<PinnedMessage>> = MutableLiveData(arrayOf())
    fun updatePeerName(peerName : String?) {
        localPeerName = peerName
    }
    override fun close() {
        if(!::hmsSessionStore.isInitialized)
            return
        hmsSessionStore.removeKeyChangeListener(keyListener, object :HMSActionResultListener{
            override fun onError(error: HMSException) {
            }

            override fun onSuccess() {

            }

        })
    }

    data class PinnedMessage(
        @SerializedName("text")
        val text : String,
        @SerializedName("id")
        val id : String,
        @SerializedName("pinnedBy")
        val pinnedBy : String
    )

    fun removeFromPinnedMessages(chatMessage: PinnedMessage, hmsActionResultListener: HMSActionResultListener) {
        val currentMessages = pinnedMessages.value?: emptyArray()
        val result = currentMessages.filter { it != chatMessage }
        updatePinnedMessage(result, hmsActionResultListener)
    }

    fun addToPinnedMessages(data: ChatMessage, hmsActionResultListener: HMSActionResultListener) {
        // text, id, pinnedBy
        val newPinnedMessage = PinnedMessage("${data.localSenderRealNameForPinMessage}: ${data.message}", data.messageId ?: "", localPeerName ?: "Participant")
        val existingPinnedMessages = pinnedMessages.value ?: arrayOf()
        val newMessages = if(existingPinnedMessages.size < MAX_PINNED_MESSAGES)
            existingPinnedMessages.plus(newPinnedMessage).toList()
        else
            existingPinnedMessages.drop(1).plus(newPinnedMessage)
        updatePinnedMessage(newMessages, hmsActionResultListener)
    }

    private fun updatePinnedMessage(data: List<PinnedMessage>, hmsActionResultListener: HMSActionResultListener) {
        hmsSessionStore.set(data, PINNED_MESSAGE_SESSION_KEY, hmsActionResultListener)
    }

    fun setPinnedMessageUpdateListener(hmsActionResultListener: HMSActionResultListener) {
        // Add the listener for the key that pinned message is sent on
        hmsSessionStore.addKeyChangeListener(listOf(PINNED_MESSAGE_SESSION_KEY),
            keyListener,hmsActionResultListener)

    }
    fun setSessionStore(sessionStore: HmsSessionStore) {
        this.hmsSessionStore = sessionStore
    }

}