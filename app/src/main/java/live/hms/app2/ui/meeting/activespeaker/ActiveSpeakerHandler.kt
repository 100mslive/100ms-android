package live.hms.app2.ui.meeting.activespeaker

import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.utils.HMSLogger

class ActiveSpeakerHandler(private val getTracks: () -> List<MeetingTrack>) {
    private val TAG = ActiveSpeakerHandler::class.java.simpleName
    private val speakerCache = ActiveSpeakerCache<SpeakerItem>(4)

    fun trackUpdateTrigger(tracks: List<MeetingTrack>): List<MeetingTrack> {
        synchronized(tracks) {
            // Update lru just to keep it as much filled as possible

            val all = tracks
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
    }

    fun speakerUpdate(speakers: Array<HMSSpeaker>): Pair<List<MeetingTrack>, Array<HMSSpeaker>> {
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

    private fun update(): List<MeetingTrack> {
        // Update all the videos which aren't screenshares

        val order = speakerCache.getAllItems()
        val videos = order.mapNotNull { orderedItem ->
            getTracks().find { givenTrack ->
                givenTrack.peer.peerID == orderedItem.peerId && givenTrack.isScreen.not()
            }
        }
        return videos
        // Always bind videos after this function is called
        // updateVideos(binding.container, videos)
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