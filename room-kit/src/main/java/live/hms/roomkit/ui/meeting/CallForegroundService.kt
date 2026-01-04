package live.hms.roomkit.ui.meeting

import android.Manifest.permission.RECORD_AUDIO
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import live.hms.roomkit.R
import live.hms.roomkit.ui.notification.CallNotificationConfig
import androidx.core.graphics.createBitmap

/**
 * Foreground service to keep the app alive during an active call when backgrounded.
 * This prevents Android 14+ from aggressively suspending network sockets.
 *
 * The service displays a persistent notification allowing users to return to the call.
 *
 * Usage:
 * - Call start() when user joins a meeting (while app is in foreground)
 * - Call updateNotification() with showDescription=true when app goes to background
 * - Call updateNotification() with showDescription=false when app comes to foreground
 * - Call stop() when user leaves the meeting
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
        private const val EXTRA_SHOW_DESCRIPTION = "extra_show_description"

        private const val ACTION_UPDATE_NOTIFICATION = "action_update_notification"

        /**
         * Start the foreground service when user joins a meeting.
         * Should be called while app is in foreground to satisfy Android 14+ requirements.
         *
         * @param context The context to start the service from
         * @param config Optional notification configuration for custom branding
         * @param showDescription Whether to show the notification description text
         */
        fun start(context: Context, config: CallNotificationConfig? = null, showDescription: Boolean = false) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                config?.let {
                    it.smallIcon?.let { icon -> putExtra(EXTRA_SMALL_ICON, icon) }
                    it.largeIcon?.let { icon -> putExtra(EXTRA_LARGE_ICON, icon) }
                    it.title?.let { title -> putExtra(EXTRA_TITLE, title) }
                    it.text?.let { text -> putExtra(EXTRA_TEXT, text) }
                    it.channelName?.let { name -> putExtra(EXTRA_CHANNEL_NAME, name) }
                    it.channelDescription?.let { desc -> putExtra(EXTRA_CHANNEL_DESCRIPTION, desc) }
                }
                putExtra(EXTRA_SHOW_DESCRIPTION, showDescription)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Update the notification to show or hide description.
         * Call with showDescription=true when app goes to background.
         * Call with showDescription=false when app comes to foreground.
         *
         * @param context The context
         * @param showDescription Whether to show the notification description text
         */
        fun updateNotification(context: Context, showDescription: Boolean) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_SHOW_DESCRIPTION, showDescription)
            }
            context.startService(intent)
        }

        /**
         * Stop the foreground service.
         * Call this when user leaves the meeting.
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
    private var showDescription: Boolean = false

    private var isServiceStarted = false

    override fun onCreate() {
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            // Just update the notification, don't restart the service
            showDescription = intent.getBooleanExtra(EXTRA_SHOW_DESCRIPTION, false)
            updateNotificationDisplay()
            return START_NOT_STICKY
        }

        // Extract config from intent
        intent?.let {
            smallIconRes = it.getIntExtra(EXTRA_SMALL_ICON, R.drawable.ic_app_logo)
            largeIconRes = it.getIntExtra(EXTRA_LARGE_ICON, R.drawable.ic_camera_toggle_off)
            notificationTitle = it.getStringExtra(EXTRA_TITLE)
            notificationText = it.getStringExtra(EXTRA_TEXT)
            channelName = it.getStringExtra(EXTRA_CHANNEL_NAME)
            channelDescription = it.getStringExtra(EXTRA_CHANNEL_DESCRIPTION)
            showDescription = it.getBooleanExtra(EXTRA_SHOW_DESCRIPTION, false)
        }

        // Create channel after extracting config (channel name may be customized)
        createNotificationChannel()

        val notification = createNotification()

        if (!isServiceStarted) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Check if we have microphone permission to determine service type
                    val hasMicPermission = checkSelfPermission(RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    val serviceType = if (hasMicPermission) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    }
                    Log.d("CallFGService", "Using service type: ${if (hasMicPermission) "MICROPHONE" else "MEDIA_PLAYBACK"}")
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        serviceType
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isServiceStarted = true
                Log.d("CallFGService", "Foreground service started successfully")
            } catch (e: SecurityException) {
                // On Android 14+, service type may fail if app is not in eligible state
                Log.e("CallFGService", "Failed to start foreground service", e)
                stopSelf()
            }
        } else {
            Log.d("CallFGService", "Service already started, skipping startForeground")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
        isServiceStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateNotificationDisplay() {
        if (isServiceStarted) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
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
            val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle ?: getString(R.string.call_notification_title))
            .setSmallIcon(smallIconRes)
            .setLargeIcon(largeIconBitmap)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        // Only show description text when app is in background
        if (showDescription) {
            builder.setContentText(notificationText ?: getString(R.string.call_notification_text))
        }

        return builder.build()
    }
}
