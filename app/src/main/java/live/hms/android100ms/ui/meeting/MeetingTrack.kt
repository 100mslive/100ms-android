package live.hms.android100ms.ui.meeting

import live.hms.video.HMSPeer
import live.hms.video.HMSRTCMediaStream
import live.hms.video.webrtc.HMSRTCAudioTrack
import live.hms.video.webrtc.HMSRTCVideoTrack

data class MeetingTrack(
  val mediaId: String,
  val peer: HMSPeer,
  val stream: HMSRTCMediaStream,
  val isCurrentDeviceStream: Boolean = false,
  val isScreen: Boolean = false,
) {

  val audioTrack: HMSRTCAudioTrack? =
    if (stream.audioTracks.size > 0) stream.audioTracks[0] else null
  val videoTrack: HMSRTCVideoTrack? =
    if (stream.videoTracks.size > 0) stream.videoTracks[0] else null

  override fun equals(other: Any?): Boolean {
    if (other is MeetingTrack) {
      return other.peer.uid == peer.uid &&
          other.mediaId == mediaId
    }

    return super.equals(other)
  }

  override fun toString(): String {
    return "MeetingTrack(" +
        "name=${peer.userName}, " +
        "mid=$mediaId, " +
        "customerUserId=${peer.customerUserId}, " +
        "hasVideo=${videoTrack != null}, " +
        "hasAudio=${audioTrack != null}, " +
        "isCurrentDeviceStream=$isCurrentDeviceStream, " +
        "isScreen=$isScreen" +
        ")"
  }
}
