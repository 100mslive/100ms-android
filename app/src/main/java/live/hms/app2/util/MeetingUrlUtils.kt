package live.hms.app2.util

import android.os.Environment
import live.hms.app2.BuildConfig

fun String.isValidMeetingUrl(): Boolean =
  this.matches(REGEX_MEETING_URL_ROOM_ID) || this.matches(REGEX_MEETING_URL_CODE)

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