package live.hms.roomkit.ui.meeting

import com.google.gson.JsonElement
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.utils.GsonUtils
import java.io.Closeable

private const val PINNED_MESSAGE_SESSION_KEY: String = "pinnedMessages"
class SessionMetadataUseCase(private val hmsSessionStore: HmsSessionStore) : Closeable {
    private val addedListeners = mutableListOf<HMSKeyChangeListener>()
    override fun close() {
        addedListeners.forEach {
            hmsSessionStore.removeKeyChangeListener(it, object :HMSActionResultListener{
                override fun onError(error: HMSException) {
                }

                override fun onSuccess() {

                }

            })
        }
        addedListeners.clear()
    }

    fun updatePinnedMessage(data: List<String>?, hmsActionResultListener: HMSActionResultListener) {
        hmsSessionStore.set(data, PINNED_MESSAGE_SESSION_KEY, hmsActionResultListener)
    }

    fun setPinnedMessageUpdateListener(pinnedMessageUpdated: (List<String>?) -> Unit, hmsActionResultListener: HMSActionResultListener) {
        // Add the listener for the key that pinned message is sent on
        val listener = object : HMSKeyChangeListener {
            override fun onKeyChanged(key: String, value: JsonElement?) {
                if(key == PINNED_MESSAGE_SESSION_KEY) {
                    // If the value was null, leave it null. Only stringify if it isn't.
                    val message = if (value == null) {
                        null
                    } else {
                        GsonUtils.gson.fromJson(value.asJsonArray, Array<String>::class.java).toList()
                    }
                    pinnedMessageUpdated(message)
                }
            }
        }
        addedListeners.add(listener)
        hmsSessionStore.addKeyChangeListener(listOf(PINNED_MESSAGE_SESSION_KEY),
            listener,hmsActionResultListener)

    }
}