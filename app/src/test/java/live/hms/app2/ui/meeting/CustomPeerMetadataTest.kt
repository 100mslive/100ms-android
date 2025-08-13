package live.hms.app2.ui.meeting

import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class CustomPeerMetadataTest{

    @Test
    fun correct_json_values_are_parsed() {
        val result = CustomPeerMetadata.fromJson("{\"isBRBOn\":true, \"name\":\"Aniket\"}")
        assertThat(result, equalTo(CustomPeerMetadata(true, "Aniket", null, null)))
    }

    @Test
    fun non_json_strings_are_are_discarded() {
        val result = CustomPeerMetadata.fromJson("hi")
        assertThat(result, equalTo(null))
    }

    @Test
    fun unexpected_json_values_are_discarded() {
        val result = CustomPeerMetadata.fromJson("{\"isBRBOn\":[34.0]}")
        assertThat(result, equalTo(null))
    }

}