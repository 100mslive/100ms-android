package live.hms.app2.ui.home

import live.hms.app2.api.TokenService
import live.hms.app2.api.makeTokenService
import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import retrofit2.Response

class HomeRepository {

  private var service: TokenService? = null
  private var currentServiceEndpoint: String = ""

  @Synchronized
  private fun getService(endpoint: String): TokenService {
    if (service == null || currentServiceEndpoint != endpoint) {
      service = makeTokenService(endpoint)
    }

    currentServiceEndpoint = endpoint
    return service!!
  }

  suspend fun fetchAuthToken(
    endpoint: String,
    request: TokenRequest
  ): Response<TokenResponse> {
    return getService(endpoint).fetchAuthToken(request)
  }
}