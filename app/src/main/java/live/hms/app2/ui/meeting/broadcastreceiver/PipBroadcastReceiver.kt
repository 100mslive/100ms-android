package live.hms.app2.ui.meeting.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import live.hms.app2.ui.meeting.MeetingFragment

class PipBroadcastReceiver(val toogleLocalAudio : () -> Unit, val disconnectCall : () -> Unit)  : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.hasExtra(MeetingFragment.muteTogglePipEvent))
            toogleLocalAudio()
        else if (intent.hasExtra(MeetingFragment.disconnectCallPipEvent))
            disconnectCall()
    }
}