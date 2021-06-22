package live.hms.app2.util

import live.hms.app2.BuildConfig

fun String.isValidMeetingUrl(): Boolean =
  this.matches(REGEX_MEETING_URL_ROOM_ID) || this.matches(REGEX_MEETING_URL_CODE)

fun String.getTokenEndpointEnvironment(): String = when {
  this.contains("prod2.100ms.live") -> "prod-in"
  !BuildConfig.INTERNAL -> "prod-in"
  else -> "qa-in"
}

fun String.getInitEndpointEnvironment(): String = when {
  this.contains("prod2.100ms.live") -> ENV_PROD
  !BuildConfig.INTERNAL -> ENV_PROD
  else -> ENV_QA
}

fun String.toValidMeetingUrl(environment: String, role: String): String {
  if (this.isValidMeetingUrl()) return this
  if (this.matches(REGEX_MEETING_CODE)) {
    // Valid Code
    return when (environment) {
      ENV_PROD -> "https://prod2.100ms.live/meeting/$this"
      else -> "https://qa2.100ms.live/meeting/$this"
    }
  } else if (this.matches(REGEX_MEETING_ROOM_ID)) {
    // Valid roomId
    return when (environment) {
      ENV_PROD -> "https://prod2.100ms.live/meeting/$this/$role"
      else -> "https://qa2.100ms.live/meeting/$this/$role"
    }
  }

  throw IllegalStateException("Invalid Room Id or Code")
}

fun String.toUniqueRoomSpecifier(): String {
  require(this.isValidMeetingUrl()) {
    "$this is not a valid meeting-url"
  }

  return if (REGEX_MEETING_URL_CODE.matches(this)) {
    val groups = REGEX_MEETING_URL_CODE.findAll(this).toList()[0].groupValues
    groups[1]
  } else /* if (REGEX_MEETING_URL_ROOM_ID.matches(this)) */ {
    val groups = REGEX_MEETING_URL_ROOM_ID.findAll(this).toList()[0].groupValues
    groups[1]
  }
}
