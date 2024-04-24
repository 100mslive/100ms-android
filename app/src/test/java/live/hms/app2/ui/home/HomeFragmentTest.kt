package live.hms.app2.ui.home

import junit.framework.TestCase
import live.hms.app2.util.REGEX_MEETING_URL_CODE
import live.hms.app2.util.REGEX_PREVIEW_URL_CODE
import live.hms.app2.util.REGEX_STREAMING_MEETING_URL_ROOM_CODE
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class HomeFragmentTest {
    @Test
    fun `get correct room code from streaming meeting`() {
        val code = getRoomCodeFromURl("https://ls-satvik.qa-app.100ms.live/streaming/meeting/ixc-opzt-uxdp")
        assertThat(code, equalTo("ixc-opzt-uxdp"))
    }

    @Test
    fun `get correct room code from meeting`() {
        val code = getRoomCodeFromURl("https://sarvesh.app.100ms.live/meeting/sun-eis-qsm")
        assertThat(code, equalTo("sun-eis-qsm"))
    }

}

private fun getRoomCodeFromURl(url: String): String? {
    return when {
        REGEX_MEETING_URL_CODE.matches(url) -> {
            val groups = REGEX_MEETING_URL_CODE.findAll(url).toList()[0].groupValues
            groups[groups.size - 1]
        }
        REGEX_STREAMING_MEETING_URL_ROOM_CODE.matches(url) -> {
            val groups =
                REGEX_STREAMING_MEETING_URL_ROOM_CODE.findAll(url).toList()[0].groupValues
            groups[groups.size - 1]

        }
        REGEX_PREVIEW_URL_CODE.matches(url) -> {
            val groups = REGEX_PREVIEW_URL_CODE.findAll(url).toList()[0].groupValues
            groups[groups.size -1]
        }
        else -> null
    }
}