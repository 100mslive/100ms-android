package live.hms.app2.util

fun String.isValidMeetingUrl(): Boolean =
  this.matches(REGEX_MEETING_URL_ROOM_ID) || this.matches(REGEX_MEETING_URL_CODE) || this.matches(REGEX_PREVIEW_URL_CODE) || this.matches(
    REGEX_STREAMING_MEETING_URL_ROOM_CODE)

fun String.getTokenEndpointEnvironment(): String = when {
  this.contains("prod2.100ms.live") -> "prod-in"
  this.contains(".app.100ms.live") -> "prod-in"
  else -> "qa-in2"
}

fun String.getInitEndpointEnvironment(): String = when {
  this.contains("prod2.100ms.live") -> ENV_PROD
  this.contains(".app.100ms.live") -> ENV_PROD
  else -> ENV_QA
}

fun getBeamBotJoiningUrl(meetingUrl: String, roomId: String, beamBotUser: String): String {
  return "https://${meetingUrl.toSubdomain()}/preview/$roomId/$beamBotUser?token=beam_recording"
}