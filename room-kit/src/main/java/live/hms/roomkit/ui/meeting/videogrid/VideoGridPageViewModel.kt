package live.hms.roomkit.ui.meeting.videogrid

import androidx.lifecycle.ViewModel
import live.hms.roomkit.ui.meeting.MeetingTrack

class VideoGridPageViewModel : ViewModel() {
  var initialVideos = arrayOf<MeetingTrack>()
  var onVideoItemClick: ((MeetingTrack) -> Unit)? = null
}