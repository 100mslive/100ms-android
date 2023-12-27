package live.hms.roomkit.ui

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import live.hms.roomkit.ui.meeting.MeetingActivity
import live.hms.roomkit.util.LIVE_ICONS_STATUS
import live.hms.roomkit.util.LOGO_URL
import live.hms.roomkit.util.PREVIEW_SCREEN_STATUS
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.TOKEN

object HMSRoomKit {


    fun launchPrebuilt(
        roomCode: String,
        activity: Activity,
        options: HMSPrebuiltOptions? = null,
        logoUrl : String? = null,
        isLiveIconsEnabled : Boolean? = null,
        isPreviewScreenEnabled : Boolean? = null
    ) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(ROOM_CODE, roomCode)
            putExtra(ROOM_PREBUILT, options)
            putExtra(LOGO_URL,logoUrl)
            putExtra(LIVE_ICONS_STATUS, isLiveIconsEnabled)
            putExtra(PREVIEW_SCREEN_STATUS, isPreviewScreenEnabled)
            startActivity(activity, this, null)
        }

    }


    fun launchPrebuiltUsingAuthToken(
        token: String,
        activity: Activity,
        options: HMSPrebuiltOptions? = null,
        logoUrl : String? = null,
        isLiveIconsEnabled : Boolean? = null,
        isPreviewScreenEnabled : Boolean? = null
    ) {
        Intent(activity, MeetingActivity::class.java).apply {
            putExtra(TOKEN, token)
            putExtra(ROOM_PREBUILT, options)
            putExtra(LOGO_URL,logoUrl)
            putExtra(LIVE_ICONS_STATUS, isLiveIconsEnabled)
            putExtra(PREVIEW_SCREEN_STATUS, isPreviewScreenEnabled)
            startActivity(activity, this, null)
        }
    }

}

