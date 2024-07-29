package live.hms.roomkit.ui.meeting.pinnedvideo

import android.text.TextUtils
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
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

    private var videoTrack: HMSVideoTrack? = null
    private var audioTrack: HMSAudioTrack? = null

    //special for simulcast case, on publishing side the track id updates hence init is not called
    //we need a way to update the video track id so that is shows the correct stats
    fun updateVideoTrack(currentVideoTrack: HMSVideoTrack?) {
        videoTrack = currentVideoTrack
    }

    fun initiateStats(
        lifecycleOwner: LifecycleOwner,
        itemStats: Flow<Map<String, Any>>,
        currentVideoTrack: HMSVideoTrack?,
        currentAudioTrack: HMSAudioTrack?,
        isLocal: Boolean,
        setText: (CharSequence) -> Unit
    ) {
        audioTrack = currentAudioTrack
        videoTrack = currentVideoTrack
        if (active) {
            lifecycleOwner.lifecycleScope.launch {

                itemStats.map { allStats ->

                    val relevantStats = mutableListOf<HMSStats?>().apply {
                         add(allStats[audioTrack?.trackId] as? HMSStats)
                        add(allStats[videoTrack?.trackId] as? HMSStats)
                        (allStats[videoTrack?.trackId] as? List<*>)?.forEach { simulcastVideoTrack ->
                            add(simulcastVideoTrack as? HMSStats)
                        }
                    }
                    return@map (relevantStats.filterNotNull())
                }
                    .map {
                        it.fold("" as CharSequence) { acc, webrtcStats ->
                            val out = when (webrtcStats) {
                                is HMSRemoteAudioStats -> buildSpannedString {
                                    append("\nAudio")
                                    appendLine()
                                    append("Jitter:${webrtcStats.jitter}")
                                    appendLine()
                                    append("Bitrate(A):${webrtcStats.bitrate?.roundToInt()}")
                                    appendLine()
                                    append("PL:${webrtcStats.packetsLost}")
                                    appendLine()
                                }
                                is HMSRemoteVideoStats -> buildSpannedString {
                                    append("\nVideo")
                                    appendLine()
                                    append("Width:${webrtcStats.resolution?.width}")
                                    appendLine()
                                    append("Height:${webrtcStats.resolution?.height}")
                                    appendLine()
                                    append("FPS:${webrtcStats.frameRate}")
                                    appendLine()
                                    append("Bitrate(V): ${webrtcStats.bitrate?.roundToInt()}")
                                    appendLine()
                                    append("PL:${webrtcStats.packetsLost}\n")
                                    appendLine()
                                    append("Jitter:${webrtcStats.jitter}")
                                    appendLine()
                                }
                                is HMSLocalAudioStats -> buildSpannedString {
                                    append("\nLocalAudio")
                                    appendLine()
                                    append("Bitrate(A): ${webrtcStats.bitrate?.roundToInt()}")
                                    appendLine()
                                    append("Jitter: ${webrtcStats.jitter}")
                                    appendLine()
                                }
                                is HMSLocalVideoStats -> buildSpannedString {
                                    append("\nLocalVideo")
                                    appendLine()
                                    if (webrtcStats.hmsLayer != null) {
                                        bold { append("${webrtcStats.hmsLayer}") }
                                        appendLine()
                                    }
                                    append("Width:${webrtcStats.resolution?.width}")
                                    appendLine()
                                    append("Height:${webrtcStats.resolution?.height}")
                                    appendLine()
                                    append("FPS: ${webrtcStats.frameRate}")
                                    appendLine()
                                    append("Bitrate(V): ${webrtcStats.bitrate?.roundToInt()}")
                                    appendLine()
                                    append("QualityLimitation:${webrtcStats.qualityLimitationReason.reason}")
                                    appendLine()
                                    append("Jitter: ${webrtcStats.jitter}")
                                    appendLine()
                                }

                                else -> acc
                            }

                            TextUtils.concat(out, acc)
                        }
                    }
                    .collect {
                        withContext(Dispatchers.Main) {
                            setText(buildString { append(it)
                                spaceBottomOfView(this)
                            })
                        }
                    }


            }
        }
    }

    private fun spaceBottomOfView(spannableStringBuilder: StringBuilder) {
        // Space out the view so it can be raised above the name
        with(spannableStringBuilder) {
            appendLine()
            appendLine()
        }
    }


}