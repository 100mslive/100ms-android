package live.hms.app2.ui.meeting

import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.signal.jsonrpc.models.sessionstore.HMSKeyChangeListener
import live.hms.video.signal.jsonrpc.models.sessionstore.HmsSessionStore

private const val PINNED_MESSAGE_SESSION_KEY: String = "pinnedMessage"
class SessionMetadataUseCase(private val hmsSessionStore: HmsSessionStore) {
    fun updatePinnedMessage(data: String?, reportError: (error: HMSException) -> Unit) {
        hmsSessionStore.set(data, PINNED_MESSAGE_SESSION_KEY, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                reportError(error)
            }

            // The listener will update the message
            override fun onSuccess() {}

        })
    }

    fun setPinnedMessageUpdateListener(pinnedMessageUpdated: (String?) -> Unit) {
        // Add the listener for the key that pinned message is sent on
        hmsSessionStore.setKeyChangeListener(listOf(PINNED_MESSAGE_SESSION_KEY),
            object : HMSKeyChangeListener {
                override fun onKeyChanged(key: String, value: String?) {
                    if(key == PINNED_MESSAGE_SESSION_KEY) {
                        pinnedMessageUpdated(value)
                    }
                }
            },
            object : HMSActionResultListener {
            override fun onError(error: HMSException) {}
            override fun onSuccess() {} })

    }
}