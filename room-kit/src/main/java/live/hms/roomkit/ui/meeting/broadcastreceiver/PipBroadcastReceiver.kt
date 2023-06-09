package live.hms.roomkit.ui.meeting.broadcastreceiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils.PIP_ACTION_EVENT
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils.disconnectCallPipEvent
import live.hms.roomkit.ui.meeting.broadcastreceiver.PipUtils.muteTogglePipEvent

class PipBroadcastReceiver(val toogleLocalAudio: () -> Unit, val disconnectCall: () -> Unit) :
    BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.hasExtra(muteTogglePipEvent))
            toogleLocalAudio()
        else if (intent.hasExtra(disconnectCallPipEvent))
            disconnectCall()
    }

    fun register(activity: Activity) {
        val filter = IntentFilter()
        filter.addAction(PIP_ACTION_EVENT)
        activity.registerReceiver(this, filter)
    }

    fun unregister(activity: Activity) {
        activity.unregisterReceiver(this)
    }
}