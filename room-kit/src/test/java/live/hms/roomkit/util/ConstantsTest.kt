package live.hms.roomkit.util

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ConstantsTest {

    @Test
    fun `constants have expected values`() {
        assertThat(ROOM_DETAILS, equalTo("room-details"))
        assertThat(ROOM_CODE, equalTo("room-code"))
        assertThat(TOKEN, equalTo("token"))
        assertThat(ROOM_PREBUILT, equalTo("room-prebuilt"))
        assertThat(MEETING_URL, equalTo("meeting-url"))
        assertThat(USERNAME, equalTo("username"))
        assertThat(ENVIRONMENT, equalTo("room-endpoint"))
        assertThat(AUTH_TOKEN, equalTo("auth-token"))
        assertThat(ENV_PROD, equalTo("prod-init"))
        assertThat(ENV_QA, equalTo("qa-init"))
        assertThat(POLL_IDENTIFIER_FOR_HLS_CUE, equalTo("poll:"))
    }

    @Test
    fun `regex patterns are not null`() {
        assertThat(REGEX_MEETING_URL_CODE, notNullValue())
        assertThat(REGEX_PREVIEW_URL_CODE, notNullValue())
        assertThat(REGEX_STREAMING_MEETING_URL_ROOM_CODE, notNullValue())
        assertThat(REGEX_MEETING_URL_ROOM_ID, notNullValue())
        assertThat(REGEX_TOKEN_ENDPOINT, notNullValue())
        assertThat(REGEX_MEETING_CODE, notNullValue())
        assertThat(REGEX_MEETING_ROOM_ID, notNullValue())
    }

    @Test
    fun `REGEX_MEETING_URL_CODE matches valid meeting URLs`() {
        val validUrls = listOf(
            "https://app.100ms.live/meeting/abc-def-ghi",
            "http://test.100ms.live/meeting/123-456-789",
            "https://custom.100ms.live/meeting/aaa-bbb-ccc/"
        )
        
        validUrls.forEach { url ->
            assertThat("$url should match", REGEX_MEETING_URL_CODE.matches(url), equalTo(true))
        }
    }

    @Test
    fun `REGEX_MEETING_URL_CODE does not match invalid meeting URLs`() {
        val invalidUrls = listOf(
            "https://app.100ms.live/meeting/",
            "https://app.100ms.live/meeting/abc",
            "https://app.100ms.live/meeting/abc-def",
            "https://different.com/meeting/abc-def-ghi"
        )
        
        invalidUrls.forEach { url ->
            assertThat("$url should not match", REGEX_MEETING_URL_CODE.matches(url), equalTo(false))
        }
    }

    @Test
    fun `REGEX_PREVIEW_URL_CODE matches valid preview URLs`() {
        val validUrls = listOf(
            "https://app.100ms.live/preview/abc-def-ghi",
            "http://test.100ms.live/preview/123-456-789",
            "https://custom.100ms.live/preview/aaa-bbb-ccc/"
        )
        
        validUrls.forEach { url ->
            assertThat("$url should match", REGEX_PREVIEW_URL_CODE.matches(url), equalTo(true))
        }
    }

    @Test
    fun `REGEX_STREAMING_MEETING_URL_ROOM_CODE matches valid streaming URLs`() {
        val validUrls = listOf(
            "https://app.100ms.live/streaming/meeting/abc-def-ghi",
            "http://test.100ms.live/streaming/meeting/123-456-789",
            "https://custom.100ms.live/streaming/meeting/aaa-bbb-ccc/"
        )
        
        validUrls.forEach { url ->
            assertThat("$url should match", REGEX_STREAMING_MEETING_URL_ROOM_CODE.matches(url), equalTo(true))
        }
    }

    @Test
    fun `REGEX_MEETING_URL_ROOM_ID matches valid room ID URLs`() {
        val validUrls = listOf(
            "https://app.100ms.live/meeting/abc123/def456",
            "http://test.100ms.live/meeting/room1/user2",
            "https://custom.100ms.live/meeting/aaa/bbb/"
        )
        
        validUrls.forEach { url ->
            assertThat("$url should match", REGEX_MEETING_URL_ROOM_ID.matches(url), equalTo(true))
        }
    }

    @Test
    fun `REGEX_TOKEN_ENDPOINT matches valid token endpoints`() {
        val validUrls = listOf(
            "https://prod.100ms.live/hmsapi/test.app.100ms.live",
            "http://qa.100ms.live/hmsapi/staging.100ms.live",
            "https://custom.100ms.live/hmsapi/my-app.100ms.live/"
        )
        
        validUrls.forEach { url ->
            assertThat("$url should match", REGEX_TOKEN_ENDPOINT.matches(url), equalTo(true))
        }
    }

    @Test
    fun `REGEX_MEETING_CODE matches valid meeting codes`() {
        val validCodes = listOf(
            "abc-def-ghi",
            "123-456-789",
            "aaa-bbb-ccc",
            "a1b2c3-d4e5f6-g7h8i9"
        )
        
        validCodes.forEach { code ->
            assertThat("$code should match", REGEX_MEETING_CODE.matches(code), equalTo(true))
        }
    }

    @Test
    fun `REGEX_MEETING_CODE does not match invalid meeting codes`() {
        val invalidCodes = listOf(
            "abc",
            "abc-def",
            "abc-def-ghi-jkl",
            "abc def ghi",
            "abc_def_ghi",
            ""
        )
        
        invalidCodes.forEach { code ->
            assertThat("$code should not match", REGEX_MEETING_CODE.matches(code), equalTo(false))
        }
    }

    @Test
    fun `REGEX_MEETING_ROOM_ID matches valid room IDs`() {
        val validIds = listOf(
            "abc123",
            "roomID456",
            "123456789",
            "aAbBcC123"
        )
        
        validIds.forEach { id ->
            assertThat("$id should match", REGEX_MEETING_ROOM_ID.matches(id), equalTo(true))
        }
    }

    @Test
    fun `REGEX_MEETING_ROOM_ID does not match invalid room IDs`() {
        val invalidIds = listOf(
            "room-id",
            "room_id",
            "room id",
            "room@id",
            ""
        )
        
        invalidIds.forEach { id ->
            assertThat("$id should not match", REGEX_MEETING_ROOM_ID.matches(id), equalTo(false))
        }
    }
}