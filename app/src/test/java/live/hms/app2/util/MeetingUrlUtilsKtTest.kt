package live.hms.app2.util

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class MeetingUrlUtilsKtTest {
    @Test
    fun `meeting streaming works`() {
        assertThat("https://masterclass.app.100ms.live/streaming/meeting/amp-pat-cat".isValidMeetingUrl(), equalTo(true))
    }

    @Test
    fun `streaming preview links work`() {
        assertThat("https://masterclass.app.100ms.live/streaming/preview/amp-pat-cat".isValidMeetingUrl(), equalTo(true))
    }

    @Test
    fun `regular meeting links work`() {
        assertThat("https://aniket.app.100ms.live/meeting/amp-pat-cat".isValidMeetingUrl(), equalTo(true))
    }

    @Test
    fun `regular preview links work`() {
        assertThat("https://aniket.app.100ms.live/preview/amp-pat-cat".isValidMeetingUrl(), equalTo(true))
    }

    @Test
    fun `broken links do not work`() {
        assertThat("https://aniket.app.100ms.live/preview/milk/amp-pat-cat".isValidMeetingUrl(), equalTo(false))
    }
}