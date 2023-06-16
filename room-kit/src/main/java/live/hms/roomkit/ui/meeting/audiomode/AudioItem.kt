package live.hms.roomkit.ui.meeting.audiomode

data class AudioItem(
  val peerId: String,
  val name: String,
  val isTrackMute: Boolean,
  val audioLevel: Int = 0,
) {

  fun clone() = AudioItem(peerId, name, isTrackMute, audioLevel)
}