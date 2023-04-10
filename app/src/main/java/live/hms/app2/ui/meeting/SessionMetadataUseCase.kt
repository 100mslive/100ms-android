package live.hms.app2.ui.meeting

import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HmsSessionStore
import live.hms.video.sessionstore.HMSKeyChangeListener

private const val PINNED_MESSAGE_SESSION_KEY: String = "pinnedMessage"
class SessionMetadataUseCase(private val hmsSessionStore: HmsSessionStore) {

    fun updatePinnedMessage(data: String?, hmsActionResultListener: HMSActionResultListener) {
        hmsSessionStore.set(data, PINNED_MESSAGE_SESSION_KEY, hmsActionResultListener)
    }

    fun setPinnedMessageUpdateListener(pinnedMessageUpdated: (String?) -> Unit, hmsActionResultListener: HMSActionResultListener) {
        // Add the listener for the key that pinned message is sent on
        hmsSessionStore.addKeyChangeListener(listOf(PINNED_MESSAGE_SESSION_KEY),
            object : HMSKeyChangeListener {
                override fun onKeyChanged(key: String, value: String?) {
                    if(key == PINNED_MESSAGE_SESSION_KEY) {
                        pinnedMessageUpdated(value)
                    }
                }
            },hmsActionResultListener)

    }
}