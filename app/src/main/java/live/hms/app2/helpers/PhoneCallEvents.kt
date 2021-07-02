package live.hms.app2.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

sealed class PhoneCallEvents {
    object MUTE_ALL : PhoneCallEvents()
    object UNMUTE_ALL : PhoneCallEvents()
}

/**
 * Emits events for whether the calls should be muted:
 *  1. Mute when a call is received or in the process of dialling.
 *  2. Unmute when a call is terminated.
 *
 *  The events will be used to determine that the video/mic should be turned off.
 *  Collecting this flow will register the receiver.
 */
@ExperimentalCoroutinesApi
fun getPhoneStateFlow(context: Context) = callbackFlow {

    // Create phone call broadcast receivers
    val regularPhoneCallReceiver: BroadcastReceiver = PhoneCallMonitor { event -> sendBlocking(event) }
    val phoneCallIntentFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)

    // Register phone receiver when the flow starts (consider shareIn so it's not duplicated)
    context.registerReceiver(regularPhoneCallReceiver, phoneCallIntentFilter)

    // Unregister receivers when the flow is closed.
    awaitClose {
        context.unregisterReceiver(regularPhoneCallReceiver)
    }
}

/**
 * A broadcast receiver that converts phone events
 * to events saying whether we should mute or not.
 */
private class PhoneCallMonitor(private val onEvent: (PhoneCallEvents) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_IDLE -> onEvent(PhoneCallEvents.UNMUTE_ALL)
            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_RINGING -> onEvent(PhoneCallEvents.MUTE_ALL)
            else -> {
                /** Ignore **/
            }
        }
    }

}
