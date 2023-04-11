package live.hms.app2.ui.meeting

import android.util.Log
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.sessionstore.HmsSessionStore

private const val PINNED_TRACK_CHANGED_KEY = "spotlight"
class PinnedTrackUseCase(private val hmsSessionStore: HmsSessionStore) {

    fun updatePinnedTrack(peerId : String?, hmsActionResultListener: HMSActionResultListener) {
        hmsSessionStore.set(peerId, PINNED_TRACK_CHANGED_KEY, hmsActionResultListener)
    }

    fun setPinnedTrackListener(pinnedTrackChanged : (String?) -> Unit, hmsActionResultListener: HMSActionResultListener) {

        hmsSessionStore.addKeyChangeListener(listOf(PINNED_TRACK_CHANGED_KEY),
            object : HMSKeyChangeListener {
                override fun onKeyChanged(key: String, value: String?) {
                    Log.d("PinnedTrackUIUseCase","Received value ${key}/$value")
                    pinnedTrackChanged(value)
                }

            },
            hmsActionResultListener
        )
    }

}