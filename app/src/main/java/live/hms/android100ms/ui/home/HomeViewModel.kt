package live.hms.android100ms.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.android100ms.api.Resource
import live.hms.android100ms.model.CreateRoomRequest
import live.hms.android100ms.model.CreateRoomResponse
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.model.TokenResponse
import live.hms.android100ms.util.handleResponse

class HomeViewModel : ViewModel() {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()
  val createRoomResponse = MutableLiveData<Resource<CreateRoomResponse>>()

  fun sendAuthTokenRequest(request: TokenRequest) {
    viewModelScope.launch {
      authTokenResponse.postValue(Resource.loading())
      val response = repository.fetchAuthToken(request)
      authTokenResponse.postValue(
        handleResponse(
          response,
          "Could not fetch auth token. Try again"
        )
      )
    }
  }

  fun sendCreateRoomRequest(request: CreateRoomRequest) {
    viewModelScope.launch {
      createRoomResponse.postValue(Resource.loading())
      val response = repository.createRoom(request)
      createRoomResponse.postValue(
        handleResponse(
          response,
          "Could not fetch create room. Try again"
        )
      )
    }
  }
}