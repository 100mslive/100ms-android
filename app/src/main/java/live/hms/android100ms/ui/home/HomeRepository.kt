package live.hms.android100ms.ui.home

import android.util.Log
import live.hms.android100ms.api.TokenApiService
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.model.TokenResponse
import retrofit2.Response

class HomeRepository {
    val TAG = "HomeRepository"

    suspend fun fetchAuthToken(tokenRequest: TokenRequest): Response<TokenResponse> {
        return TokenApiService.fetchAuthToken(tokenRequest)
    }
}