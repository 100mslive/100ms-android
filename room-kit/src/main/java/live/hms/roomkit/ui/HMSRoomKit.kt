package live.hms.roomkit.ui

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import live.hms.roomkit.ui.diagnostic.DiagnosticActivity
import live.hms.roomkit.ui.meeting.MeetingActivity
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.TOKEN

object HMSRoomKit {


    fun launchPrebuilt(roomCode: String, activity: Activity, options: HMSPrebuiltOptions? = null) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(ROOM_CODE, roomCode)
            putExtra(ROOM_PREBUILT, options)
            startActivity(activity, this, null)
        }

    }


    fun launchPrebuiltUsingAuthToken(
        token: String, activity: Activity, options: HMSPrebuiltOptions? = null
    ) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(TOKEN, token)
            putExtra(ROOM_PREBUILT, options)
            startActivity(activity, this, null)
        }
    }

    fun launchPreCallDiagnostic(activity: Activity) {
        Intent(activity, DiagnosticActivity::class.java).apply {
            startActivity(activity, this, null)
        }
    }

}

