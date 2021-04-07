package live.hms.app2.api

import live.hms.app2.model.CreateRoomRequest
import live.hms.app2.model.CreateRoomResponse
import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenService {
  @POST("token")
  suspend fun fetchAuthToken(@Body tokenRequest: TokenRequest): Response<TokenResponse>

  @POST("room_token")
  suspend fun createRoom(@Body createRoomRequest: CreateRoomRequest): Response<CreateRoomResponse>
}