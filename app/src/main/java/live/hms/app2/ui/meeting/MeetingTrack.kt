package live.hms.app2.ui.meeting

import live.hms.video.media.tracks.HMSAudioTrack
import live.hms.video.media.tracks.HMSVideoTrack

data class MeetingTrack(
  val mediaId: String,
  val video: HMSVideoTrack?,
  val audio: HMSAudioTrack?,
  val isCurrentDeviceStream: Boolean,
  val isScreen: Boolean = false,
) {

  override fun equals(other: Any?): Boolean {
    if (other is MeetingTrack) {
      return other.mediaId == mediaId
    }

    return super.equals(other)
  }
}
