package live.hms.app2.ui.meeting

import live.hms.video.error.HMSException
import live.hms.video.sdk.models.HMSRemovedFromRoom
import live.hms.video.sdk.models.HMSRoleChangeRequest

// TODO: Provide a way to bind a message with each state
//  such that the UI can be updated with proper message as well.

sealed class MeetingState {

  data class Connecting(val heading: String, val message: String) : MeetingState()
  data class Joining(val heading: String, val message: String) : MeetingState()
  data class LoadingMedia(val heading: String, val message: String) : MeetingState()
  data class PublishingMedia(val heading: String, val message: String) : MeetingState()
  data class Ongoing(val message: String = "") : MeetingState()
  data class Reconnected(val message: String = "") : MeetingState()
  data class Disconnecting(val heading: String, val message: String) : MeetingState()
  data class Reconnecting(val heading: String, val message: String) : MeetingState()
  data class Disconnected(val goToHome: Boolean = false, val removedFromRoom : HMSRemovedFromRoom? = null) : MeetingState()

  data class Failure(val exceptions: ArrayList<HMSException>) : MeetingState()
  data class RoleChangeRequest(val hmsRoleChangeRequest: HMSRoleChangeRequest) : MeetingState()
  data class ForceLeave(val details : HMSRemovedFromRoom) : MeetingState()
  data class NonFatalFailure(val exception: HMSException) : MeetingState()

}

