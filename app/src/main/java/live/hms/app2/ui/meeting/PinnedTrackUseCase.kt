package live.hms.app2.ui.meeting

import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sessionstore.HMSKeyChangeListener
import live.hms.video.sessionstore.HmsSessionStore
import java.io.Closeable

private const val PINNED_TRACK_CHANGED_KEY = "spotlight"
class PinnedTrackUseCase(private val hmsSessionStore: HmsSessionStore) : Closeable {
    private val addedListeners = mutableListOf<HMSKeyChangeListener>()

    fun updatePinnedTrack(trackId : String?, hmsActionResultListener: HMSActionResultListener) {
        hmsSessionStore.set(trackId, PINNED_TRACK_CHANGED_KEY, hmsActionResultListener)
    }

    fun setPinnedTrackListener(pinnedTrackChanged : (String?) -> Unit, hmsActionResultListener: HMSActionResultListener) {
        val listener = object : HMSKeyChangeListener {
            override fun onKeyChanged(key: String, value: Any?) {
                pinnedTrackChanged(value as String?)
            }

        }
        addedListeners.add(listener)
        hmsSessionStore.addKeyChangeListener(listOf(PINNED_TRACK_CHANGED_KEY),
            listener,
            hmsActionResultListener
        )
    }

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

}