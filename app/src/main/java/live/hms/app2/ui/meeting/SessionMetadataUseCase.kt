package live.hms.app2.ui.meeting

import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.jsonrpc.models.sessionstore.HMSKeyChangeListener

private const val PINNED_MESSAGE_SESSION_KEY: String = "pinnedMessage"
class SessionMetadataUseCase {
    fun updatePinnedMessage(hmsSDK : HMSSDK, data : String?, reportError : (error : HMSException) -> Unit) {
        hmsSDK.setSessionMetaData(
            PINNED_MESSAGE_SESSION_KEY,
            data,
            object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    reportError(error)
                }

                // The listener will update the message
                override fun onSuccess() {}

            }
        )
    }

    fun setPinnedMessageUpdateListener(hmsSDK: HMSSDK, pinnedMessageUpdated : (String?) -> Unit) {
        // Add the listener for the key that pinned message is sent on
        hmsSDK.setMetadataListener(listOf(PINNED_MESSAGE_SESSION_KEY), object : HMSActionResultListener {
            override fun onError(error: HMSException) {}
            override fun onSuccess() {} })

        // When the value changes, update the message
        hmsSDK.getSessionStore()?.keyChangeListener = object : HMSKeyChangeListener {
            override fun onKeyChanged(key: String, value: String?) {
                if(key == PINNED_MESSAGE_SESSION_KEY) {
                    pinnedMessageUpdated(value)
                }
            }
        }
    }
}