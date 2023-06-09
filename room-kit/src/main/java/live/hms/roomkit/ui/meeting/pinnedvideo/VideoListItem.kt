package live.hms.roomkit.ui.meeting.pinnedvideo

import live.hms.roomkit.ui.meeting.MeetingTrack

data class VideoListItem(
  val id: Long,
  val track: MeetingTrack,
  val isTrackMute: Boolean
)