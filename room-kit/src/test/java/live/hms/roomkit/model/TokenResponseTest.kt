package live.hms.roomkit.model

import com.google.gson.Gson
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TokenResponseTest {

    private val gson = Gson()

    @Test
    fun `data class properties work correctly`() {
        val token = "test-token-123"
        val response = TokenResponse(token)
        
        assertThat(response.token, equalTo(token))
    }

    @Test
    fun `serialization to JSON works correctly`() {
        val response = TokenResponse("my-token")
        val json = gson.toJson(response)
        
        assertThat(json, equalTo("{\"token\":\"my-token\"}"))
    }

    @Test
    fun `deserialization from JSON works correctly`() {
        val json = "{\"token\":\"test-token-value\"}"
        val response = gson.fromJson(json, TokenResponse::class.java)
        
        assertThat(response, notNullValue())
        assertThat(response.token, equalTo("test-token-value"))
    }

    @Test
    fun `equals and hashCode work correctly`() {
        val response1 = TokenResponse("token1")
        val response2 = TokenResponse("token1")
        val response3 = TokenResponse("token2")
        
        assertThat(response1, equalTo(response2))
        assertThat(response1.hashCode(), equalTo(response2.hashCode()))
        assertThat(response1 == response3, equalTo(false))
    }

    @Test
    fun `copy function works correctly`() {
        val original = TokenResponse("original-token")
        val copy = original.copy(token = "new-token")
        
        assertThat(copy.token, equalTo("new-token"))
        assertThat(original.token, equalTo("original-token"))
    }

    @Test
    fun `toString includes token value`() {
        val response = TokenResponse("test-token")
        val stringRepresentation = response.toString()
        
        assertThat(stringRepresentation.contains("test-token"), equalTo(true))
        assertThat(stringRepresentation.contains("TokenResponse"), equalTo(true))
    }
}