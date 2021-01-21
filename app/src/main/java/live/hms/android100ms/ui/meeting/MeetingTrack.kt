package live.hms.android100ms.ui.meeting

import com.brytecam.lib.HMSPeer
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

data class MeetingTrack(
  val peer: HMSPeer,
  val videoTrack: VideoTrack?,
  val audioTrack: AudioTrack?,
  val isCurrentDeviceStream: Boolean = false
) {
  override fun equals(other: Any?): Boolean {
    if (other is MeetingTrack) {
      return other.peer.customerUserId == peer.customerUserId
    }

    return super.equals(other)
  }

  override fun toString(): String {
    val peerStr = "HMSPeer(" +
        "peerId=${peer.peerId}, " +
        "username=${peer.userName}, " +
        "customerUserId=${peer.customerUserId}" +
        ")"
    return "MeetingTrack(" +
        "${peerStr}, " +
        "${videoTrack}, " +
        "${audioTrack}, " +
        "isCurrentDeviceStream=${isCurrentDeviceStream}" +
        ")"
  }
}
