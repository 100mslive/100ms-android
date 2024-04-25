package live.hms.roomkit.ui.meeting.activespeaker

import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.utils.HMSLogger
import java.util.concurrent.ConcurrentLinkedDeque

class ActiveSpeakerHandler(appendUnsorted : Boolean = false, numActiveSpeakerVideos : Int = 4, private val getTracks: () -> ConcurrentLinkedDeque<MeetingTrack>) {
    private val TAG = ActiveSpeakerHandler::class.java.simpleName
    private val speakerCache = ActiveSpeakerCache<SpeakerItem>(numActiveSpeakerVideos, appendUnsorted)

    fun trackUpdateTrigger(tracks: ConcurrentLinkedDeque<MeetingTrack>, removeLocal : Boolean = false): ConcurrentLinkedDeque<MeetingTrack> {
            // Update lru just to keep it as much filled as possible

            val all = tracks
                .filter { !it.isScreen && !(removeLocal && it.isLocal) }
                .sortedByDescending {
                    if (it.audio == null || it.audio?.isMute == true || it.isScreen) {
                        it.peer.name.hashCode() * -1 // Drop these ids really low.
                    } else
                        it.peer.name.hashCode()
                }
                .map {
                    SpeakerItem(it.peer.peerID, it.peer.name)
                }

            speakerCache.update(all, false)

            return update()
    }

    fun speakerUpdate(speakers: Array<HMSSpeaker>): Pair<ConcurrentLinkedDeque<MeetingTrack>, Array<HMSSpeaker>> {
        HMSLogger.v(
            TAG,
            "speakers update received 🎙 [size=${speakers.size}, names=${speakers.map { it.peer?.name }}] "
        )

        speakerCache.update(
            speakers.filter { it.peer != null } // Sometimes the peer which the server says is speaking, might be missing.
                .map { SpeakerItem(it.peer!!.peerID, it.peer!!.name) },
            true
        )
        return Pair(update(), speakers)
    }

    private fun update(): ConcurrentLinkedDeque<MeetingTrack> {
        // Update all the videos which aren't screenshares

        val order : ConcurrentLinkedDeque<SpeakerItem> = speakerCache.getAllItems()
        // Find the speaker in the list of tracks, filtering out those tracks which are screens.
        val trackMap = getTracks().filter { !it.isScreen }.associateBy { it.peer.peerID }
        return order.mapNotNull { trackMap[it.peerId] }.toCollection(ConcurrentLinkedDeque<MeetingTrack>())
    }

    fun updateMaxActiveSpeaker(maxActiveSpeaker: Int) {
        speakerCache.updateMaxActiveSpeaker(maxActiveSpeaker)
    }

    data class SpeakerItem(
        val peerId: String,
        val peerName: String
    ) {
        override fun toString(): String {
            return peerName
        }
    }

}