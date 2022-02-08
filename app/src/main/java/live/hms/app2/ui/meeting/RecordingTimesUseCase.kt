package live.hms.app2.ui.meeting

import live.hms.video.sdk.models.HMSRoom
import java.text.SimpleDateFormat
import java.util.*

class RecordingTimesUseCase() {
    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    fun showServerInfo(room : HMSRoom): MeetingViewModel.Event.ServerRecordEvent {
        val startStop =
            convertTimes(room.serverRecordingState?.startedAt, null)
        return MeetingViewModel.Event.ServerRecordEvent("Server Started: ${startStop.first}")
    }

    fun showRecordInfo(room : HMSRoom): MeetingViewModel.Event.RecordEvent {
        val startStop = convertTimes(room.browserRecordingState?.startedAt, room.browserRecordingState?.stoppedAt)

        return MeetingViewModel.Event.RecordEvent("Recording Started: ${startStop.first}, Stopped: ${startStop.second}")
    }

    fun showRtmpInfo(room : HMSRoom): MeetingViewModel.Event.RtmpEvent {
        val startStop = convertTimes(room.rtmpHMSRtmpStreamingState?.startedAt, room.rtmpHMSRtmpStreamingState?.stoppedAt)
        val rtmp = "Rtmp Started: ${startStop.first}, Stopped: ${startStop.second}"

        return MeetingViewModel.Event.RtmpEvent(rtmp)
    }

    fun convertTimes(startedAt : Long?, stoppedAt: Long?) : Pair<String, String> {
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