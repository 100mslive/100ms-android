package live.hms.roomkit.ui.meeting.activespeaker.portablehls

import com.google.android.exoplayer2.PlaybackException


public data class HmsHlsException(
    val error : PlaybackException
)