package live.hms.android100ms.ui.meeting

// TODO: Provide a way to bind a message with each state
//  such that the UI can be updated with proper message as well.

sealed class MeetingState {

  data class Disconnected(
    val showDialog: Boolean = false,
    val heading: String = "",
    val message: String = "",

    val goToHome: Boolean = false,
  ) : MeetingState()

  data class Connecting(val heading: String, val message: String) : MeetingState()
  data class Joining(val heading: String, val message: String) : MeetingState()
  data class LoadingMedia(val heading: String, val message: String) : MeetingState()
  data class PublishingMedia(val heading: String, val message: String) : MeetingState()
  data class Ongoing(val message: String = "") : MeetingState()
  data class Disconnecting(val heading: String, val message: String) : MeetingState()
}

