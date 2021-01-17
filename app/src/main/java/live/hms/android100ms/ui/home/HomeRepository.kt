package live.hms.android100ms.ui.home

import live.hms.android100ms.api.TokenApiService
import live.hms.android100ms.model.CreateRoomRequest
import live.hms.android100ms.model.CreateRoomResponse
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.model.TokenResponse
import retrofit2.Response

class HomeRepository {

    suspend fun fetchAuthToken(request: TokenRequest): Response<TokenResponse> {
        return TokenApiService.fetchAuthToken(request)
    }

    suspend fun createRoom(request: CreateRoomRequest): Response<CreateRoomResponse> {
        return TokenApiService.createRoom(request)
    }
}