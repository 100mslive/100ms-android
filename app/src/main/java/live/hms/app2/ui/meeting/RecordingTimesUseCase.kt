package live.hms.app2.ui.meeting

import live.hms.video.sdk.models.HMSRoom
import java.text.SimpleDateFormat
import java.util.*

class RecordingTimesUseCase() {
    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    fun showServerInfo(room : HMSRoom): String {
        val startStop =
            convertTimes(room.serverRecordingState?.startedAt, null)
        return "Server\nStarted: ${startStop.first}"
    }

    fun showRecordInfo(room : HMSRoom): String {
        val startStop = convertTimes(room.browserRecordingState?.startedAt, room.browserRecordingState?.stoppedAt)

        return "Recording\nStarted:${startStop.first}\nStopped:${startStop.second}"
    }

    fun showRtmpInfo(room : HMSRoom): String {
        val startStop = convertTimes(room.rtmpHMSRtmpStreamingState?.startedAt, room.rtmpHMSRtmpStreamingState?.stoppedAt)
        return "Rtmp\nStarted:${startStop.first}\nStopped:${startStop.second}"
    }

    fun showHlsInfo(room: HMSRoom, isRecordingEvent: Boolean) : String {
        val prefix = if(isRecordingEvent) "RecordingEvent:" else "StreamingEvent:"
        return "$prefix: HLS Streaming: ${room.hlsStreamingState?.running}, Recording: ${room.hlsRecordingState?.running}, Variants: ${room.hlsStreamingState?.variants}, Recording Config: ${room.hlsRecordingState?.hlsRecordingConfig}"
    }

    private fun convertTimes(startedAt : Long?, stoppedAt: Long?) : Pair<String, String> {
        val startedAt = if(startedAt == null)
            "Empty"
        else
            dateFormat.format(startedAt)

        val stoppedAt = if(stoppedAt == null)
            "Empty"
        else
            dateFormat.format(stoppedAt)

        return Pair(startedAt, stoppedAt)
    }

}