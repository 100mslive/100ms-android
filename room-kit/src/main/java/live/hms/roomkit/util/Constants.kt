package live.hms.roomkit.util

const val ROOM_DETAILS = "room-details"
const val ROOM_CODE = "room-code"
const val TOKEN = "token"
const val ROOM_PREBUILT = "room-prebuilt"
const val MEETING_URL = "meeting-url"
const val USERNAME = "username"
const val ENVIRONMENT = "room-endpoint"
const val AUTH_TOKEN = "auth-token"

const val ENV_PROD = "prod-init"
const val ENV_QA = "qa-init"

val REGEX_MEETING_URL_CODE = Regex("https?://(.*.100ms.live)/meeting/([a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+)/?")
val REGEX_PREVIEW_URL_CODE = Regex("https?://(.*.100ms.live)/preview/([a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+)/?")
val REGEX_STREAMING_MEETING_URL_ROOM_CODE = Regex("https?://(.*.100ms.live)/streaming/meeting/([a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+)/?")
val REGEX_MEETING_URL_ROOM_ID = Regex("https?://(.*.100ms.live)/meeting/([a-zA-Z0-9]+)/([a-zA-Z0-9]+)/?")
val REGEX_TOKEN_ENDPOINT = Regex("https?://.*.100ms.live/hmsapi/([a-zA-Z0-9-.]+.100ms.live)/?")

val REGEX_MEETING_CODE = Regex("^[a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+$")
val REGEX_MEETING_ROOM_ID = Regex("^[a-zA-Z0-9]+$")