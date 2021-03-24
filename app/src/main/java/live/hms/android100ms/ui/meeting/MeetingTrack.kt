package live.hms.android100ms.ui.meeting

import live.hms.video.HMSPeer
import live.hms.video.webrtc.HMSRTCAudioTrack
import live.hms.video.webrtc.HMSRTCVideoTrack

data class MeetingTrack(
  val mediaId: String,
  val peer: HMSPeer,
  val videoTrack: HMSRTCVideoTrack?,
  val audioTrack: HMSRTCAudioTrack?,
  val isCurrentDeviceStream: Boolean = false,
  val isScreen: Boolean = false,
) {

  override fun equals(other: Any?): Boolean {
    if (other is MeetingTrack) {
      return other.peer.peerId == peer.peerId &&
          other.mediaId == mediaId &&
          other.videoTrack == videoTrack &&
          other.audioTrack == audioTrack
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
        "mid=$mediaId, " +
        "peer=$peerStr, " +
        "hasVideo=${videoTrack != null}, " +
        "hasAudio=${audioTrack != null}, " +
        "isCurrentDeviceStream=$isCurrentDeviceStream, " +
        "isScreen=$isScreen" +
        ")"
  }
}
