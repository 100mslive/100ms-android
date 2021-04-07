package live.hms.app2.ui.home

import live.hms.app2.api.TokenApiService
import live.hms.app2.model.CreateRoomRequest
import live.hms.app2.model.CreateRoomResponse
import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import retrofit2.Response

class HomeRepository {

  suspend fun fetchAuthToken(request: TokenRequest): Response<TokenResponse> {
    return TokenApiService.fetchAuthToken(request)
  }

  suspend fun createRoom(request: CreateRoomRequest): Response<CreateRoomResponse> {
    return TokenApiService.createRoom(request)
  }
}