package live.hms.roomkit.ui

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import live.hms.roomkit.model.RoomDetails
import live.hms.roomkit.ui.meeting.MeetingActivity
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_DETAILS
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import java.util.*
import kotlin.collections.HashMap

object HMSRoomKit {


    fun launchPrebuilt(roomCode: String, activity: Activity, options: HMSPrebuiltOptions? = null) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(ROOM_CODE, roomCode)
            putExtra(ROOM_PREBUILT, options)
            startActivity(activity, this, null)
        }

    }

}

