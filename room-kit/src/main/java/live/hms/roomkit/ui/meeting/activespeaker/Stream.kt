package live.hms.roomkit.ui.meeting.activespeaker

sealed class Stream {
    object Started : Stream()
    object Stopped: Stream()
}
