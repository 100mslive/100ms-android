package live.hms.app2.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.hms.app2.api.Resource
import live.hms.app2.api.RetrofitBuilder
import live.hms.app2.model.TokenResponse
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.signal.init.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import okhttp3.Request
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  val authTokenResponse = MutableLiveData<Resource<TokenResponse>>()

}