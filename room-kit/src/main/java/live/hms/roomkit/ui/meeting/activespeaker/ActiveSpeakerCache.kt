package live.hms.roomkit.ui.meeting.activespeaker

import java.util.concurrent.ConcurrentLinkedQueue

class ActiveSpeakerCache<T>(val capacity: Int) {
    private val TAG = ActiveSpeakerCache::class.java.simpleName
    private val speakers = ConcurrentLinkedQueue<T>()

    fun update(items: List<T>, isActiveSpeakerUpdate: Boolean): Unit = synchronized(speakers) {
        // These are all the items that should be in here.
        val newList = if (isActiveSpeakerUpdate) {
            (items.take(capacity) union speakers).take(capacity)
        } else {
            // First remove any tracks that have gotten removed.
            //  Which are the tracks present in speakers but not in items
            val excludeRemovedTracks = speakers.intersect(items)

            // Only fill items, not remove any further items.
            (excludeRemovedTracks union items.take(capacity)).take(capacity)
        }
        speakers.clear()
        speakers.addAll(newList)
    }

    fun getAllItems(): List<T> = synchronized(speakers) { speakers.toList() }

}