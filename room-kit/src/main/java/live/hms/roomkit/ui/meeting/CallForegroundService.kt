package live.hms.roomkit.ui.meeting

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service to keep the app alive during an active call when backgrounded.
 * This prevents Android 14+ from suspending network sockets.
 *
 * TODO: Implement in Phase 2
 */
class CallForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
