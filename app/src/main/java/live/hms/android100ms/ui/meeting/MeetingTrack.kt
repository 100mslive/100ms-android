package live.hms.android100ms.ui.meeting

import com.brytecam.lib.HMSPeer
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class MeetingTrack(
    val peer: HMSPeer,
    val videoTrack: VideoTrack?,
    val audioTrack: AudioTrack?,
    val isCurrentDeviceStream: Boolean = false
)
