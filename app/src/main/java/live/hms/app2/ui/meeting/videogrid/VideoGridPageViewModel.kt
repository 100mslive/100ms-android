package live.hms.app2.ui.meeting.videogrid

import androidx.lifecycle.ViewModel
import live.hms.app2.ui.meeting.MeetingTrack

class VideoGridPageViewModel : ViewModel() {
  var initialVideos = arrayOf<MeetingTrack>()
  var onVideoItemClick: ((MeetingTrack) -> Unit)? = null
}