package live.hms.app2.ui.home

import live.hms.app2.api.RetrofitBuilder
import live.hms.app2.model.TokenResponse
import okhttp3.Request
import retrofit2.Response

class HomeRepository {

  suspend fun fetchAuthToken(request: Request): TokenResponse {
    return RetrofitBuilder.fetchAuthToken(request)
  }
}