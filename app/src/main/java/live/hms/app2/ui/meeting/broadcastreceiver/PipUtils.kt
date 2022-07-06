package live.hms.app2.ui.meeting.broadcastreceiver

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent

internal object PipUtils {

    const val PIP_ACTION_EVENT = "PIP_ACTION_EVENT"
    const val disconnectCallPipEvent = "disconnectCall"
    const val muteTogglePipEvent = "muteToggle"

    fun getToggleMuteBroadcast(activity: Activity): PendingIntent = PendingIntent.getBroadcast(
        activity,
        345,
        Intent(PIP_ACTION_EVENT).putExtra(disconnectCallPipEvent, 345),
        PendingIntent.FLAG_IMMUTABLE
    )


    fun getEndCallBroadcast(activity: Activity) = PendingIntent.getBroadcast(
        activity,
        344,
        Intent(PIP_ACTION_EVENT).putExtra(muteTogglePipEvent, 344),
        PendingIntent.FLAG_IMMUTABLE
    )

}