package live.hms.roomkit.model

import com.google.gson.Gson
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class RecordingInfoTest {

    private val gson = Gson()

    @Test
    fun `data class properties work correctly with true`() {
        val recordingInfo = RecordingInfo(true)
        assertThat(recordingInfo.enabled, equalTo(true))
    }

    @Test
    fun `data class properties work correctly with false`() {
        val recordingInfo = RecordingInfo(false)
        assertThat(recordingInfo.enabled, equalTo(false))
    }

    @Test
    fun `serialization to JSON works correctly when enabled`() {
        val recordingInfo = RecordingInfo(true)
        val json = gson.toJson(recordingInfo)
        
        assertThat(json, equalTo("{\"enabled\":true}"))
    }

    @Test
    fun `serialization to JSON works correctly when disabled`() {
        val recordingInfo = RecordingInfo(false)
        val json = gson.toJson(recordingInfo)
        
        assertThat(json, equalTo("{\"enabled\":false}"))
    }

    @Test
    fun `deserialization from JSON works correctly when enabled`() {
        val json = "{\"enabled\":true}"
        val recordingInfo = gson.fromJson(json, RecordingInfo::class.java)
        
        assertThat(recordingInfo, notNullValue())
        assertThat(recordingInfo.enabled, equalTo(true))
    }

    @Test
    fun `deserialization from JSON works correctly when disabled`() {
        val json = "{\"enabled\":false}"
        val recordingInfo = gson.fromJson(json, RecordingInfo::class.java)
        
        assertThat(recordingInfo, notNullValue())
        assertThat(recordingInfo.enabled, equalTo(false))
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val info1 = RecordingInfo(true)
        val info2 = RecordingInfo(true)
        val info3 = RecordingInfo(false)
        
        assertThat(info1, equalTo(info2))
        assertThat(info1.hashCode(), equalTo(info2.hashCode()))
        assertThat(info1 == info3, equalTo(false))
    }

    @Test
    fun `copy function works correctly`() {
        val original = RecordingInfo(true)
        val copy = original.copy(enabled = false)
        
        assertThat(copy.enabled, equalTo(false))
        assertThat(original.enabled, equalTo(true))
    }

    @Test
    fun `toString includes enabled value`() {
        val recordingInfo = RecordingInfo(true)
        val stringRepresentation = recordingInfo.toString()
        
        assertThat(stringRepresentation.contains("true"), equalTo(true))
        assertThat(stringRepresentation.contains("RecordingInfo"), equalTo(true))
    }
}