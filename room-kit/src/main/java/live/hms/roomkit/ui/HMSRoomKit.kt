package live.hms.roomkit.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import live.hms.roomkit.api.Resource
import live.hms.roomkit.model.RoomDetails
import live.hms.roomkit.model.TokenResponse
import live.hms.roomkit.ui.meeting.MeetingActivity
import live.hms.roomkit.util.ROOM_DETAILS
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import java.util.*

object HMSRoomKit {


    fun launchPrebuilt(roomCode: String, context: Activity, options: HMSPrebuiltOptions? = null) {
        val sdkInstance = HMSSDK.Builder(context.application).build()

        sendAuthTokenRequestCode(
            sdkInstance,
            roomCode,
            options?.environment.orEmpty(),
            options?.userName.orEmpty(),
            activity = context
        )
    }


    private fun sendAuthTokenRequestCode(
        sdkInstance: HMSSDK,
        code: String,
        environment: String,
        userName: String,
        activity: Activity?
    ) {


        val baseURl: String =
            if (environment.contains("prod").not()) "https://auth-nonprod.100ms.live" else ""

        sdkInstance.getAuthTokenByRoomCode(TokenRequest(code, UUID.randomUUID().toString()),
            TokenRequestOptions(baseURl),
            object : HMSTokenListener {
                override fun onError(error: HMSException) {
                    //  authTokenResponse.postValue(Resource.error(error.description))
                }

                override fun onTokenSuccess(token: String) {

                    val roomDetails = RoomDetails(
                        env = environment, url = "", username = userName, authToken = token
                    )

                    if (activity != null && !activity.isFinishing) {
                        startMeetingActivity(roomDetails, activity)
                    }

                }

            })

    }

    private fun startMeetingActivity(roomDetails: RoomDetails, activity: Activity) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(ROOM_DETAILS, roomDetails)
            startActivity(activity, this, null)
        }
    }


}

