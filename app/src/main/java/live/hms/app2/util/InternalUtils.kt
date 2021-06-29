package live.hms.app2.util


fun String.toSubdomain(): String {
  // ------------------ IGNORE BLOCK START ------------------
  if (this.contains("prod2.100ms.live")) {
    return "internal.app.100ms.live"
  } else if (this.contains("qa2.100ms.live")) {
    return "internal.qa-app.100ms.live"
  }
  // ------------------ IGNORE BLOCK END --------------------

  val regex = when {
    REGEX_MEETING_URL_ROOM_ID.matches(this) -> REGEX_MEETING_URL_ROOM_ID
    REGEX_MEETING_URL_CODE.matches(this) -> REGEX_MEETING_URL_CODE
    REGEX_TOKEN_ENDPOINT.matches(this) -> REGEX_TOKEN_ENDPOINT
    else -> throw IllegalStateException("$this is not a valid base token endpoint")
  }

  val groups = regex.findAll(this)
  return groups.toList()[0].groupValues[1]
}

fun getTokenEndpointForRoomId(environment: String, subdomain: String): String {
  return "https://$environment.100ms.live/hmsapi/$subdomain/api/token"
}

fun getTokenEndpointForCode(environment: String): String {
  return "https://$environment.100ms.live/hmsapi/get-token"
}

