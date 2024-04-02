package live.hms.app2.util

fun String.isValidMeetingUrl(): Boolean =
  this.matches(REGEX_MEETING_URL_ROOM_ID) || this.matches(REGEX_MEETING_URL_CODE) || this.matches(REGEX_PREVIEW_URL_CODE) || this.matches(
    REGEX_STREAMING_MEETING_URL_ROOM_CODE) || this.matches(REGEX_ROOM_CODE)

fun String.getTokenEndpointEnvironment(): String = when {
  this.contains(".qa-app.100ms.live") -> ENV_QA
  else -> ENV_PROD
}

fun String.getInitEndpointEnvironment(): String = when {
  this.contains(".qa-app.100ms.live") -> ENV_QA
  else -> ENV_PROD
}

fun String.toUniqueRoomSpecifier(): String {
  require(this.isValidMeetingUrl()) {
    "$this is not a valid meeting-url"
  }

  return if (REGEX_MEETING_URL_CODE.matches(this)) {
    val groups = REGEX_MEETING_URL_CODE.findAll(this).toList()[0].groupValues
    groups[2]
  } else /* if (REGEX_MEETING_URL_ROOM_ID.matches(this)) */ {
    val groups = REGEX_MEETING_URL_ROOM_ID.findAll(this).toList()[0].groupValues
    groups[2]
  }
}

fun getBeamBotJoiningUrl(meetingUrl: String, roomId: String, beamBotUser: String): String {
  return "https://${meetingUrl.toSubdomain()}/preview/$roomId/$beamBotUser?token=beam_recording"
}