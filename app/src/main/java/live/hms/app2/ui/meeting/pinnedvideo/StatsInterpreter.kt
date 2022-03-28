package live.hms.app2.ui.meeting.pinnedvideo

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.hms.video.connection.stats.*
import live.hms.video.media.tracks.HMSAudioTrack
import live.hms.video.media.tracks.HMSVideoTrack
import kotlin.math.roundToInt

class StatsInterpreter(val active: Boolean) {
    // For this to happen in n time rather than n^x times for a video, they'll have to
    // register with the central flow and receive events during the iteration.

    fun initiateStats(
        lifecycleOwner: LifecycleOwner,
        itemStats: Flow<Map<String, HMSStats>>,
        currentVideoTrack: HMSVideoTrack?,
        currentAudioTrack: HMSAudioTrack?,
        isLocal: Boolean,
        setText: (String) -> Unit
    ) {
        if (active) {
            lifecycleOwner.lifecycleScope.launch {

                itemStats.map { allStats ->

                    val relevantStats = mutableListOf<HMSStats?>().apply {
                        add(allStats[currentAudioTrack?.trackId])
                        add(allStats[currentVideoTrack?.trackId])
                    }
                    return@map (relevantStats.filterNotNull())
                }
                    .map {
                        it.fold("") { acc, webrtcStats ->
                            acc + when (webrtcStats) {
                                is HMSRemoteAudioStats -> "\nAudio:\n\tJitter:${webrtcStats.jitter}\n\nBytesReceived:${webrtcStats.bytesReceived}\nBitrate:${webrtcStats.bitrate?.roundToInt()}\nPR:${webrtcStats.packetsReceived}\nPL:${webrtcStats.packetsLost}\n"
                                is HMSRemoteVideoStats -> "\nVideo:\n\tJitter:${webrtcStats.jitter}\nPL:${webrtcStats.packetsLost}\nFPS:${webrtcStats.frameRate}\nWidth:${webrtcStats.resolution?.width}\nHeight:${webrtcStats.resolution?.height}\n"
                                is HMSLocalAudioStats -> "\nLocalAudio:\n\tIncoming: ${webrtcStats.bitrate?.roundToInt()}\nBytesSent: ${webrtcStats.bytesSent}\nRTT${webrtcStats.roundTripTime}"
                                is HMSLocalVideoStats -> "\nLocalVideo:\n\tIncoming: ${webrtcStats.bitrate?.roundToInt()}\nBytesSent: ${webrtcStats.bytesSent}\nRTT${webrtcStats.roundTripTime}\nWidth:${webrtcStats.resolution?.width}\nHeight:${webrtcStats.resolution?.height}"
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
    }

}