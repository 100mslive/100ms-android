package live.hms.app2.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.app2.BuildConfig
import live.hms.app2.api.Resource
import live.hms.app2.api.RetrofitBuilder
import live.hms.app2.model.TokenResponse
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import live.hms.video.signal.init.TokenResult
import live.hms.video.utils.TokenUtils
import okhttp3.Request

class HomeViewModel : ViewModel() {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

  fun sendAuthTokenRequest(url: String) {
    try {
      val env = url.getTokenEndpointEnvironment()
      val subdomain = url.toSubdomain()

      when {
        REGEX_MEETING_URL_CODE.matches(url) -> {
          val groups = REGEX_MEETING_URL_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
          if (code.length == 11)
            sendAuthTokenRequest(subdomain, code, env)
          else
            sendAuthTokenRequestCode(subdomain, code, env)


        }
        REGEX_STREAMING_MEETING_URL_ROOM_CODE.matches(url) -> {
          val groups = REGEX_STREAMING_MEETING_URL_ROOM_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
          if (code.length == 11)
            sendAuthTokenRequest(subdomain, code, env)
          else
            sendAuthTokenRequestCode(subdomain, code, env)

        }
        REGEX_PREVIEW_URL_CODE.matches(url) -> {
          val groups = REGEX_PREVIEW_URL_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
          if (code.length == 11)
            sendAuthTokenRequest(subdomain, code, env)
          else
            sendAuthTokenRequestCode(subdomain, code, env)

        }
        REGEX_MEETING_URL_ROOM_ID.matches(url) -> {
          val groups = REGEX_MEETING_URL_ROOM_ID.findAll(url).toList()[0].groupValues
          val roomId = groups[2]
          val role = groups[3]
          sendAuthTokenRequest(subdomain, roomId, role, env)

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
    subdomain: String,
    code: String,
    environment: String
  ) {
    val request = RetrofitBuilder.makeTokenWithCodeRequest(subdomain, code, environment)
    sendAuthTokenRequest(request)
  }

  private fun sendAuthTokenRequest(
    subdomain: String,
    roomId: String,
    role: String,
    environment: String
  ) {
    val request = RetrofitBuilder.makeTokenWithRoomIdRequest(subdomain, roomId, role, environment)
    sendAuthTokenRequest(request)
  }

  private fun sendAuthTokenRequestCode(
    subdomain: String,
    code: String,
    environment: String
  ) {

    var subdomainUrl = BuildConfig.TOKEN_ENDPOINT.toSubdomain()
    if (BuildConfig.INTERNAL) {

      val env = when ("") {
        ENV_PROD -> "prod2"
        else -> "qa2"
      }
      subdomainUrl = "$env.100ms.live"
    }


    TokenUtils.getAuthTokenByRoomCode(TokenRequest(code), TokenRequestOptions(subdomainUrl) , object :HMSTokenListener {
      override fun onError(error: HMSException) {
        authTokenResponse.postValue(Resource.error(error.message))
      }

      override fun onTokenSuccess(tokenResult: TokenResult) {
        authTokenResponse.postValue(Resource.success(TokenResponse(tokenResult.token.orEmpty())))
      }

    })

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