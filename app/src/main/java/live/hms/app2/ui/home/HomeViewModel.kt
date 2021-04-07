package live.hms.app2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.app2.api.Resource
import live.hms.app2.model.CreateRoomRequest
import live.hms.app2.model.CreateRoomResponse
import live.hms.app2.model.TokenRequest
import live.hms.app2.model.TokenResponse
import live.hms.app2.util.crashlytics
import live.hms.app2.util.handleResponse

class HomeViewModel : ViewModel() {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()
  val createRoomResponse = MutableLiveData<Resource<CreateRoomResponse>>()

  fun sendAuthTokenRequest(request: TokenRequest) {
    viewModelScope.launch {
      authTokenResponse.postValue(Resource.loading())
      try {
        val response = repository.fetchAuthToken(request)
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

  fun sendCreateRoomRequest(request: CreateRoomRequest) {
    viewModelScope.launch {
      createRoomResponse.postValue(Resource.loading())
      try {
        val response = repository.createRoom(request)
        createRoomResponse.postValue(
          handleResponse(
            response,
            "Could not create room. Try again"
          )
        )
      } catch (e: Exception) {
        crashlytics.recordException(e)
        authTokenResponse.postValue(Resource.error(e.message))
      }
    }
  }
}