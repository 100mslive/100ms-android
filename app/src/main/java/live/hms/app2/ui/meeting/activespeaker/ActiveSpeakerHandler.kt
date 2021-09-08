package live.hms.app2.ui.meeting.activespeaker

import live.hms.app2.ui.meeting.MeetingTrack
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.utils.HMSLogger

class ActiveSpeakerHandler(private val getTracks: () -> List<MeetingTrack>) {
    private val TAG = ActiveSpeakerHandler::class.java.simpleName
    private val lru = ActiveSpeakerCache<ActiveSpeakerFragment.LruItem>(4)

    fun trackUpdateLruTrigger(tracks: List<MeetingTrack>): List<MeetingTrack> {
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
                    ActiveSpeakerFragment.LruItem(it.peer.peerID, it.peer.name)
                }

            lru.update(all, false)

            return update()
        }
    }

    fun speakerUpdate(speakers: Array<HMSSpeaker>): Pair<List<MeetingTrack>, Array<HMSSpeaker>> {
        HMSLogger.v(
            TAG,
            "speakers update received 🎙 [size=${speakers.size}, names=${speakers.map { it.peer?.name }}] "
        )

        lru.update(
            speakers.map { ActiveSpeakerFragment.LruItem(it.peer!!.peerID, it.peer!!.name) },
            true
        )
        return Pair(update(), speakers)
    }

    private fun update(): List<MeetingTrack> {
        val order = lru.getAllItems()
        val videos = order.mapNotNull { orderedItem ->
            getTracks().find { givenTrack ->
                givenTrack.peer.peerID == orderedItem.peerId && givenTrack.isScreen.not()
            }
        }
        // Update all the videos which aren't screenshares
        return videos
        // TODO call the update videos function
//        updateVideos(binding.container, videos)
    }

}