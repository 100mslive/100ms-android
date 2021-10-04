package live.hms.app2.ui.meeting

import live.hms.video.media.tracks.*
import live.hms.video.sdk.models.HMSPeer

data class MeetingTrack(
  val peer: HMSPeer,
  var video: HMSVideoTrack?,
  var audio: HMSAudioTrack?,
) {

  override fun equals(other: Any?): Boolean {
    if (other is MeetingTrack) {
      return (other.peer.peerID == peer.peerID && other.video?.trackId == video?.trackId && other.audio?.trackId == audio?.trackId)
    }
    return super.equals(other)
  }

  val isLocal: Boolean = peer.isLocal
  val isScreen: Boolean
    get() = video?.source == HMSTrackSource.SCREEN || video?.source == "videoplaylist"
}
