package live.hms.roomkit.ui.meeting.activespeaker.portablehls

public interface HmsHlsPlaybackEvents {
    fun onPlaybackFailure(error: HmsHlsException){}
    fun onCue(cue: HmsHlsCue){}
    fun onPlaybackStateChanged(state : HmsHlsPlaybackState){}
}