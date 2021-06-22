package live.hms.app2.util

import live.hms.app2.BuildConfig


fun String.containsSubdomain(): Boolean = this.matches(REGEX_TOKEN_ENDPOINT)

fun String.toSubdomain(): String {
  require(this.containsSubdomain()) {
    "$this is not a valid base token endpoint"
  }

  val groups = REGEX_TOKEN_ENDPOINT.findAll(this)
  return groups.toList()[0].groupValues[1]
}

fun getTokenEndpointForRoomId(environment: String): String {
  val subdomain = BuildConfig.TOKEN_ENDPOINT.toSubdomain()
  return "https://$environment.100ms.live/hmsapi/$subdomain/api/token"
}

fun getTokenEndpointForCode(environment: String): String {
  return "https://$environment.100ms.live/hmsapi/get-token"
}

