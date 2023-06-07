package live.hms.app2.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import live.hms.video.signal.init.TokenResult
import okhttp3.Request
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = HomeRepository()

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

  val sdkInstance = HMSSDK
    .Builder(application)
    .build()


  fun sendAuthTokenRequest(url: String) {
    try {
      val env = url.getTokenEndpointEnvironment()
      val subdomain = url.toSubdomain()

      when {
        REGEX_MEETING_URL_CODE.matches(url) -> {
          val groups = REGEX_MEETING_URL_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
            sendAuthTokenRequestCode(subdomain, code, env)


        }
        REGEX_STREAMING_MEETING_URL_ROOM_CODE.matches(url) -> {
          val groups = REGEX_STREAMING_MEETING_URL_ROOM_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
            sendAuthTokenRequestCode(subdomain, code, env)

        }
        REGEX_PREVIEW_URL_CODE.matches(url) -> {
          val groups = REGEX_PREVIEW_URL_CODE.findAll(url).toList()[0].groupValues
          val code = groups[2]
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


    val baseURl : String = if (environment.contains("prod").not()) "https://auth-nonprod.100ms.live" else ""

    sdkInstance.getAuthTokenByRoomCode(TokenRequest(code, UUID.randomUUID().toString()), TokenRequestOptions(baseURl) , object :
      HMSTokenListener {
      override fun onError(error: HMSException) {
        authTokenResponse.postValue(Resource.error(error.description))
      }

      override fun onTokenSuccess(token: String) {
        authTokenResponse.postValue(Resource.success(TokenResponse(token.orEmpty())))
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
        authTokenResponse.postValue(Resource.error(e.message))
      }
    }
  }
}