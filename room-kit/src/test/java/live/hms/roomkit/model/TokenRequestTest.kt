package live.hms.roomkit.model

import com.google.gson.Gson
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TokenRequestTest {

    private val gson = Gson()

    @Test
    fun `TokenRequestWithRoomId properties work correctly`() {
        val roomId = "room123"
        val userId = "user456"
        val role = "host"
        
        val request = TokenRequestWithRoomId(roomId, userId, role)
        
        assertThat(request.roomId, equalTo(roomId))
        assertThat(request.userId, equalTo(userId))
        assertThat(request.role, equalTo(role))
    }

    @Test
    fun `TokenRequestWithRoomId serialization works correctly`() {
        val request = TokenRequestWithRoomId("room1", "user1", "guest")
        val json = gson.toJson(request)
        
        assertThat(json.contains("\"room_id\":\"room1\""), equalTo(true))
        assertThat(json.contains("\"user_id\":\"user1\""), equalTo(true))
        assertThat(json.contains("\"role\":\"guest\""), equalTo(true))
    }

    @Test
    fun `TokenRequestWithRoomId deserialization works correctly`() {
        val json = "{\"room_id\":\"testRoom\",\"user_id\":\"testUser\",\"role\":\"moderator\"}"
        val request = gson.fromJson(json, TokenRequestWithRoomId::class.java)
        
        assertThat(request, notNullValue())
        assertThat(request.roomId, equalTo("testRoom"))
        assertThat(request.userId, equalTo("testUser"))
        assertThat(request.role, equalTo("moderator"))
    }

    @Test
    fun `TokenRequestWithCode properties work correctly`() {
        val code = "ABC-DEF-GHI"
        val userId = "user789"
        
        val request = TokenRequestWithCode(code, userId)
        
        assertThat(request.code, equalTo(code))
        assertThat(request.userId, equalTo(userId))
    }

    @Test
    fun `TokenRequestWithCode serialization works correctly`() {
        val request = TokenRequestWithCode("XYZ-123-456", "user2")
        val json = gson.toJson(request)
        
        assertThat(json.contains("\"code\":\"XYZ-123-456\""), equalTo(true))
        assertThat(json.contains("\"user_id\":\"user2\""), equalTo(true))
    }

    @Test
    fun `TokenRequestWithCode deserialization works correctly`() {
        val json = "{\"code\":\"TEST-CODE-123\",\"user_id\":\"testUser123\"}"
        val request = gson.fromJson(json, TokenRequestWithCode::class.java)
        
        assertThat(request, notNullValue())
        assertThat(request.code, equalTo("TEST-CODE-123"))
        assertThat(request.userId, equalTo("testUser123"))
    }

    @Test
    fun `TokenRequestWithRoomId equals and hashCode work correctly`() {
        val request1 = TokenRequestWithRoomId("room1", "user1", "host")
        val request2 = TokenRequestWithRoomId("room1", "user1", "host")
        val request3 = TokenRequestWithRoomId("room2", "user1", "host")
        
        assertThat(request1, equalTo(request2))
        assertThat(request1.hashCode(), equalTo(request2.hashCode()))
        assertThat(request1 == request3, equalTo(false))
    }

    @Test
    fun `TokenRequestWithCode equals and hashCode work correctly`() {
        val request1 = TokenRequestWithCode("CODE1", "user1")
        val request2 = TokenRequestWithCode("CODE1", "user1")
        val request3 = TokenRequestWithCode("CODE2", "user1")
        
        assertThat(request1, equalTo(request2))
        assertThat(request1.hashCode(), equalTo(request2.hashCode()))
        assertThat(request1 == request3, equalTo(false))
    }

    @Test
    fun `TokenRequestWithRoomId copy function works correctly`() {
        val original = TokenRequestWithRoomId("room1", "user1", "host")
        val copy = original.copy(role = "guest")
        
        assertThat(copy.role, equalTo("guest"))
        assertThat(copy.roomId, equalTo("room1"))
        assertThat(copy.userId, equalTo("user1"))
        assertThat(original.role, equalTo("host"))
    }

    @Test
    fun `TokenRequestWithCode copy function works correctly`() {
        val original = TokenRequestWithCode("CODE1", "user1")
        val copy = original.copy(code = "CODE2")
        
        assertThat(copy.code, equalTo("CODE2"))
        assertThat(copy.userId, equalTo("user1"))
        assertThat(original.code, equalTo("CODE1"))
    }
}