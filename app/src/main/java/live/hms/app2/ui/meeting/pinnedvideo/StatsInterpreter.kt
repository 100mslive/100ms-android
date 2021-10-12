package live.hms.app2.ui.meeting.pinnedvideo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import live.hms.video.connection.degredation.Audio
import live.hms.video.connection.degredation.Peer
import live.hms.video.connection.degredation.Video
import live.hms.video.connection.degredation.WebrtcStats
import live.hms.video.media.tracks.HMSAudioTrack
import live.hms.video.media.tracks.HMSVideoTrack
import java.io.Closeable

class StatsInterpreter : Closeable {
    private val dispatcher = CoroutineScope(Dispatchers.Default)
    private var statsJob: Job? = null
    // For this to happen in n time rather than n^x times for a video, they'll have to
    // register with the central flow and receive events during the iteration.

    fun initiateStats(
        itemStats: Flow<Map<String, WebrtcStats>>,
        currentVideoTrack: HMSVideoTrack?,
        currentAudioTrack: HMSAudioTrack?,
        isLocal: Boolean,
        setText: (String) -> Unit
    ) {
        statsJob = dispatcher.launch {

            itemStats.map { allStats ->

                val relevantStats = mutableListOf<WebrtcStats?>().apply {
                    add(allStats[currentAudioTrack?.trackId])
                    add(allStats[currentVideoTrack?.trackId])
                    if (isLocal) {
                        add(allStats[Peer.LOCAL_PEER])
                    }
                }
                return@map (relevantStats.filterNotNull())
            }
                .map {
                    it.fold("") { acc, webrtcStats ->
                        acc + when (webrtcStats) {
                            is Audio -> "\nAudio:\n\tJitter:${webrtcStats.jitter}\nPL:${webrtcStats.packetsLost}\nConcealment Events:${webrtcStats.concealmentEvents}"
                            is Video -> "\nVideo:\n\tJitter:${webrtcStats.jitter}\nPL:${webrtcStats.packetsLost}\nFPS:${webrtcStats.framesPerSecond}\nFD:${webrtcStats.framesDropped}"
                            is Peer -> "\nPeer:\n\tIncoming: ${webrtcStats.availableIncomingBitrate}\nOutgoing: ${webrtcStats.availableOutgoingBitrate}\n${webrtcStats.currentRoundTripTime}"
                            else -> acc
                        }
                    }
                }
                .collect {
                    withContext(Dispatchers.Main) {
                        setText(it)
                    }
                }
        }
    }

    override fun close() {
        statsJob?.cancel()
    }

}