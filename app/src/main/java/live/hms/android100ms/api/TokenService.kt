package live.hms.android100ms.api

import live.hms.android100ms.model.CreateRoomRequest
import live.hms.android100ms.model.CreateRoomResponse
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenService {
  @POST("token")
  suspend fun fetchAuthToken(@Body tokenRequest: TokenRequest): Response<TokenResponse>

  @POST("?api=room")
  suspend fun createRoom(@Body createRoomRequest: CreateRoomRequest): Response<CreateRoomResponse>
}