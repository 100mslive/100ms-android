package live.hms.roomkit.ui.meeting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import live.hms.roomkit.R
import live.hms.roomkit.ui.notification.CallNotificationConfig

/**
 * Foreground service to keep the app alive during an active call when backgrounded.
 * This prevents Android 14+ from aggressively suspending network sockets.
 *
 * The service displays a persistent notification allowing users to return to the call.
 */
class CallForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "hms_call_channel"
        private const val NOTIFICATION_ID = 100

        private const val EXTRA_SMALL_ICON = "extra_small_icon"
        private const val EXTRA_LARGE_ICON = "extra_large_icon"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_TEXT = "extra_text"
        private const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        private const val EXTRA_CHANNEL_DESCRIPTION = "extra_channel_description"

        /**
         * Start the foreground service with optional custom notification config.
         * Call this from MeetingActivity.onStop() when user is in an active meeting.
         *
         * @param context The context to start the service from
         * @param config Optional notification configuration for custom branding
         */
        fun start(context: Context, config: CallNotificationConfig? = null) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                config?.let {
                    it.smallIcon?.let { icon -> putExtra(EXTRA_SMALL_ICON, icon) }
                    it.largeIcon?.let { icon -> putExtra(EXTRA_LARGE_ICON, icon) }
                    it.title?.let { title -> putExtra(EXTRA_TITLE, title) }
                    it.text?.let { text -> putExtra(EXTRA_TEXT, text) }
                    it.channelName?.let { name -> putExtra(EXTRA_CHANNEL_NAME, name) }
                    it.channelDescription?.let { desc -> putExtra(EXTRA_CHANNEL_DESCRIPTION, desc) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the foreground service.
         * Call this from MeetingActivity.onStart() or when leaving the meeting.
         */
        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java)
            context.stopService(intent)
        }
    }

    // Config values extracted from intent
    private var smallIconRes: Int = R.drawable.ic_app_logo
    private var largeIconRes: Int = R.drawable.ic_camera_toggle_off
    private var notificationTitle: String? = null
    private var notificationText: String? = null
    private var channelName: String? = null
    private var channelDescription: String? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Extract config from intent
        intent?.let {
            smallIconRes = it.getIntExtra(EXTRA_SMALL_ICON, R.drawable.ic_app_logo)
            largeIconRes = it.getIntExtra(EXTRA_LARGE_ICON, R.drawable.ic_camera_toggle_off)
            notificationTitle = it.getStringExtra(EXTRA_TITLE)
            notificationText = it.getStringExtra(EXTRA_TEXT)
            channelName = it.getStringExtra(EXTRA_CHANNEL_NAME)
            channelDescription = it.getStringExtra(EXTRA_CHANNEL_DESCRIPTION)
        }

        // Create channel after extracting config (channel name may be customized)
        createNotificationChannel()

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName ?: getString(R.string.call_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = channelDescription ?: getString(R.string.call_notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Create intent to return to MeetingActivity when notification is tapped
        val tapIntent = Intent(this, MeetingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Convert drawable to bitmap for large icon
        val largeIconBitmap = androidx.core.graphics.drawable.DrawableCompat.wrap(
            androidx.core.content.ContextCompat.getDrawable(this, largeIconRes)!!
        ).let { drawable ->
            val bitmap = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle ?: getString(R.string.call_notification_title))
            .setContentText(notificationText ?: getString(R.string.call_notification_text))
            .setSmallIcon(smallIconRes)
            .setLargeIcon(largeIconBitmap)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }
}
