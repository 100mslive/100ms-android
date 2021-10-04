package live.hms.app2.util

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class InternalUtilsKtTest() {

    private val meetingUrl = "https://aniket.app.100ms.live/meeting/correct-horse-battery"

    @Test
    fun testToSubdomain() {
        val subdomain = meetingUrl.toSubdomain()

//        URI(meetingUrl).host
        assertThat(subdomain, equalTo("aniket.app.100ms.live"))
    }

    @Test
    fun environment() {
        val env = meetingUrl.getTokenEndpointEnvironment()
        assertThat(env, equalTo("prod-in"))
    }

    @Test
    fun `get meeting url code`() {
        val groups = REGEX_MEETING_URL_CODE.findAll(meetingUrl).toList()[0].groupValues
        val code = groups[2]

//        val code = android.net.Uri.parse(meetingUrl).lastPathSegment
        assertThat(code, equalTo("correct-horse-battery"))
    }

    @Test
    fun `get token endpoint for join`() {
        val tokenEndpoint = getTokenEndpointForCode(meetingUrl.getTokenEndpointEnvironment())
        assertThat(tokenEndpoint, equalTo("https://prod-in.100ms.live/hmsapi/get-token"))
    }

    @Test
    fun `beam bot urls are correctly constructed`() {
        val beamJoiningUrl =
            getBeamBotJoiningUrl(meetingUrl, "61407fd3d91703e09854b8562", "beambot")
        assertThat(
            beamJoiningUrl,
            equalTo("https://aniket.app.100ms.live/preview/61407fd3d91703e09854b8562/beambot?token=beam_recording")
        )
    }
}