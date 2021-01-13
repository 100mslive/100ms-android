package live.hms.android100ms.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.android100ms.api.Resource
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.model.TokenResponse
import live.hms.android100ms.util.handleResponse

class HomeViewModel : ViewModel() {
    private val repository = HomeRepository()

    val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

    fun sendAuthTokenRequest(tokenRequest: TokenRequest) {
        viewModelScope.launch {
            authTokenResponse.postValue(Resource.loading())
            val response = repository.fetchAuthToken(tokenRequest)
            authTokenResponse.postValue(
                handleResponse(
                    response,
                    "Could not fetch auth token. Try again"
                )
            )
        }
    }
}