package live.hms.android100ms.ui.meeting.videogrid

import androidx.lifecycle.ViewModel
import live.hms.android100ms.ui.meeting.MeetingTrack

class VideoGridPageViewModel : ViewModel() {
  var initialVideos = arrayOf<MeetingTrack>()
  var onVideoItemClick: ((MeetingTrack) -> Unit)? = null


}