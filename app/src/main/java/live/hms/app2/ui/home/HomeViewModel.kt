package live.hms.app2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.app2.api.Resource
import live.hms.app2.api.RetrofitBuilder
import live.hms.app2.model.TokenResponse
import live.hms.app2.util.REGEX_MEETING_URL_CODE
import live.hms.app2.util.REGEX_MEETING_URL_ROOM_ID
import live.hms.app2.util.crashlytics
import live.hms.app2.util.getTokenEndpointEnvironment
import okhttp3.Request

class HomeViewModel : ViewModel() {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

  fun sendAuthTokenRequest(url: String) {
    try {
      val env = url.getTokenEndpointEnvironment()

      when {
        REGEX_MEETING_URL_CODE.matches(url) -> {
          val groups = REGEX_MEETING_URL_CODE.findAll(url).toList()[0].groupValues
          val code = groups[1]
          sendAuthTokenRequest(code, env)

        }
        REGEX_MEETING_URL_ROOM_ID.matches(url) -> {
          val groups = REGEX_MEETING_URL_ROOM_ID.findAll(url).toList()[0].groupValues
          val roomId = groups[1]
          val role = groups[2]
          sendAuthTokenRequest(roomId, role, env)

        }
        else -> {
          authTokenResponse.postValue(Resource.error("Invalid Meeting URL"))
        }
      }
    } catch (ex: Exception) {
      authTokenResponse.postValue(Resource.error("Invalid Meeting URL [${ex.message}]"))
    }
  }

  private fun sendAuthTokenRequest(
    roomId: String,
    role: String,
    environment: String
  ) {
    val request = RetrofitBuilder.makeTokenWithRoomIdRequest(roomId, role, environment)
    sendAuthTokenRequest(request)
  }

  private fun sendAuthTokenRequest(
    code: String,
    environment: String
  ) {
    val request = RetrofitBuilder.makeTokenWithCodeRequest(code, environment)
    sendAuthTokenRequest(request)
  }

  private fun sendAuthTokenRequest(request: Request) {
    viewModelScope.launch {
      authTokenResponse.postValue(Resource.loading())
      try {
        val response = repository.fetchAuthToken(request)
        authTokenResponse.postValue(Resource.success(response))
      } catch (e: Exception) {
        crashlytics.recordException(e)
        authTokenResponse.postValue(Resource.error(e.message))
      }
    }
  }
}