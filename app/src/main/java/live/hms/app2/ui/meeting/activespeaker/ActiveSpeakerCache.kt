package live.hms.app2.ui.meeting.activespeaker

import live.hms.video.utils.HMSLogger
import java.util.concurrent.ConcurrentLinkedQueue

class ActiveSpeakerCache<T>(val capacity: Int) {
    private val TAG = ActiveSpeakerCache::class.java.simpleName
    private val speakers = ConcurrentLinkedQueue<T>()

    fun update(items: List<T>, isActiveSpeakerUpdate: Boolean): Unit = synchronized(speakers) {
        // These are all the items that should be in here.
        if (isActiveSpeakerUpdate) {
            val prevTemp = speakers
            HMSLogger.v(TAG, "Prev:$prevTemp")
            val update = (items.take(capacity) union speakers).take(capacity)
            HMSLogger.v(TAG, "New :$update")

            speakers.clear()
            speakers.addAll(update)
            if (prevTemp != update) {
                HMSLogger.v(TAG, "List changed: Prev:$prevTemp\nNew :$update")
            }
            HMSLogger.v(TAG, "--------run finished---------")
        } else {
            // First remove any tracks that have gotten removed.
            //  Which are the tracks present in speakers but not in items
            val excludeRemovedTracks = speakers.intersect(items)

            // Only fill items, not remove any further items.
            val update = (excludeRemovedTracks union items.take(capacity)).take(capacity)

            speakers.clear()
            speakers.addAll(update)
        }
    }

    fun getAllItems(): List<T> = synchronized(speakers) { speakers.toList() }

}