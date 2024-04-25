package live.hms.roomkit.ui.meeting.activespeaker

import java.util.concurrent.ConcurrentLinkedDeque

class ActiveSpeakerCache<T>(private var capacity: Int, private val appendUnsorted : Boolean = false) {
    private val TAG = ActiveSpeakerCache::class.java.simpleName
    private val speakers = ConcurrentLinkedDeque<T>()

    fun update(items: List<T>, isActiveSpeakerUpdate: Boolean): Unit = synchronized(speakers) {
        // These are all the items that should be in here.
        val newList = if (isActiveSpeakerUpdate) {
            (items.take(capacity) union speakers)
        } else {
            // First remove any tracks that have gotten removed.
            //  Which are the tracks present in speakers but not in items
            val excludeRemovedTracks = speakers.intersect(items)

            // Only fill items, not remove any further items.
            (excludeRemovedTracks union items.take(capacity))
        }
        val finalList = if(appendUnsorted) {
            newList.plus(items.drop(capacity))
        } else {
            newList.take(capacity)
        }
        speakers.clear()
        speakers.addAll(finalList)
    }

    fun getAllItems(): ConcurrentLinkedDeque<T> = synchronized(speakers) { speakers }
    fun updateMaxActiveSpeaker(maxActiveSpeaker: Int) {
        capacity = maxActiveSpeaker
    }

}