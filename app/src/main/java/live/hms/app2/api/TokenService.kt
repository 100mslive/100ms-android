package live.hms.app2.api

import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenService {
  @POST("api/token")
  suspend fun fetchAuthToken(@Body tokenRequest: TokenRequest): Response<TokenResponse>
}