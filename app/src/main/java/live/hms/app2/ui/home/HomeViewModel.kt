package live.hms.app2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.app2.api.Resource
import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import live.hms.app2.util.crashlytics
import live.hms.app2.util.handleResponse

class HomeViewModel : ViewModel() {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

  fun sendAuthTokenRequest(endpoint: String, request: TokenRequest) {
    viewModelScope.launch {
      authTokenResponse.postValue(Resource.loading())
      try {
        val response = repository.fetchAuthToken(endpoint, request)
        authTokenResponse.postValue(
          handleResponse(
            response,
            "Could not fetch auth token. Try again"
          )
        )
      } catch (e: Exception) {
        crashlytics.recordException(e)
        authTokenResponse.postValue(Resource.error(e.message))
      }
    }
  }
}