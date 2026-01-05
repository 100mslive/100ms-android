package live.hms.roomkit.ui.meeting

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.animation.RootViewDeferringInsetsCallback
import live.hms.roomkit.databinding.ActivityMeetingBinding
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.meeting.participants.ParticipantsFragment
import live.hms.roomkit.ui.notification.CardStackLayoutManager
import live.hms.roomkit.ui.notification.CardStackListener
import live.hms.roomkit.ui.notification.Direction
import live.hms.roomkit.ui.notification.HMSNotification
import live.hms.roomkit.ui.notification.HMSNotificationAdapter
 import live.hms.roomkit.ui.notification.CallNotificationConfig
import live.hms.roomkit.ui.notification.HMSNotificationDiffCallBack
import live.hms.roomkit.ui.notification.HMSNotificationType
import live.hms.roomkit.ui.polls.display.PollDisplayFragment
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.TOKEN
import live.hms.roomkit.util.init

class MeetingActivity : AppCompatActivity() {

    var requestedPermissions: Array<String> = arrayOf()
    private var _binding: ActivityMeetingBinding? = null

    private val binding: ActivityMeetingBinding
        get() = _binding!!

    var settingsStore: SettingsStore? = null
    private var notificationManager: CardStackLayoutManager? = null
    private val hmsNotificationAdapter by lazy {
        HMSNotificationAdapter(
            onActionButtonClicked = ::handleNotificationButtonClick,
            onDismissClicked = ::handleNotificationDismissClick
        )
    }

    private val meetingViewModel: MeetingViewModel by viewModels {
        MeetingViewModelFactory(
            application,
        )
    }

    // Track if user is in an active meeting (non-HLS) for foreground service
    private var isInActiveMeeting = false

    // Notification config from HMSPrebuiltOptions for foreground service
    private var callNotificationConfig: CallNotificationConfig? = null

    // Launcher for requesting notification permission on Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result doesn't affect flow - service will start regardless. Notification just won't show if permission was denied
    }


    // Request POST_NOTIFICATIONS permission on Android 13+ if not already granted.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Updates the active meeting state based on joined status.
     * Starts the foreground service when user joins (any role).
     * Uses MICROPHONE type for users with mic permission, MEDIA_PLAYBACK for viewers.
     */
    private fun updateActiveMeetingState() {
        val joined = meetingViewModel.joined.value == true
        val wasInActiveMeeting = isInActiveMeeting
        isInActiveMeeting = joined

        Log.d("CallFGService", "updateActiveMeetingState: joined=$joined, wasInActiveMeeting=$wasInActiveMeeting, isInActiveMeeting=$isInActiveMeeting")

        // Start service when user joins meeting (while app is still in foreground)
        if (isInActiveMeeting && !wasInActiveMeeting) {
            try {
                CallForegroundService.start(this, callNotificationConfig, showDescription = false)
            } catch (e: Exception) {
                Log.e("CallFGService", "Failed to start foreground service on join", e)
            }
        } else {
           Log.d("CallFGService", "Not starting service: isInActiveMeeting=$isInActiveMeeting, wasInActiveMeeting=$wasInActiveMeeting")
        }

        // Stop service when user leaves the meeting
        if (!isInActiveMeeting && wasInActiveMeeting) {
            CallForegroundService.stop(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        settingsStore = SettingsStore(this)

        // Request notification permission on Android 13+ for foreground service notification
        requestNotificationPermissionIfNeeded()

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        // RootViewDeferringInsetsCallback is both an WindowInsetsAnimation.Callback and an
        // OnApplyWindowInsetsListener, so needs to be set as so.
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, deferringInsetsListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)


        val hmsPrebuiltOption: HMSPrebuiltOptions? =
            intent!!.extras!![ROOM_PREBUILT] as? HMSPrebuiltOptions

        // Store notification config for foreground service
        callNotificationConfig = hmsPrebuiltOption?.callNotificationConfig

        val roomCode: String = intent?.getStringExtra(ROOM_CODE)?:""
        val token: String = intent?.getStringExtra(TOKEN)?:""

        if (roomCode.isEmpty() && token.isEmpty()) {
            Toast.makeText(this, "Room code or token is required", Toast.LENGTH_SHORT).show()
            finish()
        }


        binding.progressBar.visibility = View.VISIBLE
        meetingViewModel.initSdk(roomCode, token, hmsPrebuiltOption, null)

        initObservers()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Permissions handling
        lifecycleScope.launch {
            meetingViewModel.events.collect { event ->
                if (event is MeetingViewModel.Event.RequestPermission) {
                    // Add POST_NOTIFICATIONS to the permission request on Android 13+
                    // This is needed for foreground service notification to be visible
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        event.permissions + POST_NOTIFICATIONS
                    } else {
                        event.permissions
                    }
                    requestedPermissions = permissions
                    requestPermissionLauncher.launch(permissions)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // App came to foreground - update notification to hide description
        if (isInActiveMeeting) {
            CallForegroundService.updateNotification(this, showDescription = false)
        }
    }

    override fun onStop() {
        super.onStop()
        // App going to background - update notification to show description
        // Don't update if activity is finishing (user leaving)
        if (isInActiveMeeting && !isFinishing) {
            CallForegroundService.updateNotification(this, showDescription = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure service is stopped when activity is destroyed
        CallForegroundService.stop(this)
        _binding = null
    }

    private fun initObservers() {
        // Start foreground service when user joins the meeting (any role)
        meetingViewModel.joined.observe(this) { updateActiveMeetingState() }
        meetingViewModel.recordingState.observe(this) {
            invalidateOptionsMenu()
        }
        meetingViewModel.streamingState.observe(this) {
            invalidateOptionsMenu()
        }
        meetingViewModel.pinnedTrack.observe(this) {
            if (it != null) Toast.makeText(this, "Spotlight: ${it.peer.name}", Toast.LENGTH_SHORT)
                .show()
        }

        meetingViewModel.hmsNotificationEvent.observe(this) { notification ->
            triggerNotification(notification)
            notification.autoRemoveTypeAfterMillis?.let {
                lifecycleScope.launch {
                    delay(it)
                    tryRemovingAllNotificationsOfType(notification.type)
                }
            }
        }

        meetingViewModel.hmsRemoveNotificationEvent.observe(this) {
            tryRemovingNotification(it)
        }

        meetingViewModel.roomLayoutLiveData.observe(this) {success ->
            if(success) {
                binding.progressBar.visibility = View.GONE
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                val topFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                val navGraph = navController.navInflater.inflate(R.navigation.meeting_nav_graph)
                if (meetingViewModel.shouldSkipPreview().not()) {
                  navGraph.setStartDestination(R.id.PreviewFragment)
                }
                else {
                  navGraph.setStartDestination(R.id.MeetingFragment)
                }
                navController.setGraph(navGraph, intent.extras)
            } else {
                Toast.makeText(this@MeetingActivity, "Error while Getting Room Layout Data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        meetingViewModel.launchParticipantsFromHls.observe(this) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, ParticipantsFragment())
                .commit()
        }
    }

    private fun triggerNotification(hmsNotification: HMSNotification) {
        initNotificationUI()
        appendNotification(hmsNotification)
    }


    private fun appendNotification(hmsNotification: HMSNotification) {
        binding.notifcationCardList.visibility = View.VISIBLE
        //hack
        val old = hmsNotificationAdapter.getItems()
        if (hmsNotification.type == HMSNotificationType.ScreenShare) {
           val hasScreenShareType = old.find { it.type == HMSNotificationType.ScreenShare }
           if (hasScreenShareType != null) {
               return
           }
        }

        val new = mutableListOf<HMSNotification>().apply {
            addAll(old)
            add(notificationManager!!.topPosition, hmsNotification)

        }
        val callback = HMSNotificationDiffCallBack(old, new)
        val result = DiffUtil.calculateDiff(callback)
        hmsNotificationAdapter.setItems(new)
        result.dispatchUpdatesTo(hmsNotificationAdapter)
    }

    private fun tryRemovingAllNotificationsOfType(HMSNotificationType: HMSNotificationType) {

        if (hmsNotificationAdapter.getItems().isEmpty()) {
            return
        }

        val old = hmsNotificationAdapter.getItems()

        val getMatchingNotfictionTypeIndexToRemove = hmsNotificationAdapter.getItems().filter { it.type == HMSNotificationType }
        if (getMatchingNotfictionTypeIndexToRemove.isEmpty())
            return

        val new = mutableListOf<HMSNotification>().apply {
            addAll(old)
            removeAll(getMatchingNotfictionTypeIndexToRemove)

        }
        val callback = HMSNotificationDiffCallBack(old, new)
        val result = DiffUtil.calculateDiff(callback)
        hmsNotificationAdapter.setItems(new)
        result.dispatchUpdatesTo(hmsNotificationAdapter)
    }
    private fun tryRemovingNotification(HMSNotificationType: HMSNotificationType) {

        if (hmsNotificationAdapter.getItems().isEmpty()) {
            return
        }

        val old = hmsNotificationAdapter.getItems()

        val getMatchingNotfictionTypeIndexToRemove = hmsNotificationAdapter.getItems().indexOfFirst { it.type == HMSNotificationType }
        if (getMatchingNotfictionTypeIndexToRemove == -1)
            return

        val new = mutableListOf<HMSNotification>().apply {
            addAll(old)
            removeAt(getMatchingNotfictionTypeIndexToRemove)

        }
        val callback = HMSNotificationDiffCallBack(old, new)
        val result = DiffUtil.calculateDiff(callback)
        hmsNotificationAdapter.setItems(new)
        result.dispatchUpdatesTo(hmsNotificationAdapter)
    }


    private fun initNotificationUI() {
        if (notificationManager == null && binding.notifcationCardList?.context != null) {
            notificationManager = notificationManager.init(this, object : CardStackListener{
                override fun onCardDragging(direction: Direction?, ratio: Float) {}

                override fun onCardSwiped(direction: Direction?) {}

                override fun onCardRewound() {}

                override fun onCardCanceled() {}

                override fun onCardAppeared(view: View?, position: Int) {
                    binding.notifcationCardList.visibility = View.VISIBLE
                }

                override fun onCardDisappeared(view: View?, position: Int) {
                    if ((notificationManager?.topPosition?:0) + 1 == hmsNotificationAdapter.itemCount) {
                      binding.notifcationCardList.visibility = View.GONE
                    }
                }

            })
            binding.notifcationCardList?.apply {
                layoutManager = notificationManager
                adapter = hmsNotificationAdapter
                itemAnimator.apply {
                    if (this is DefaultItemAnimator) {
                        supportsChangeAnimations = false
                    }
                }
            }
        }

    }

    private fun handleNotificationButtonClick(type: HMSNotificationType) {
        when (type) {
            is HMSNotificationType.BringOnStage -> {
                meetingViewModel.requestBringOnStage(type.handRaisePeer, type.onStageRole)
                handleNotificationDismissClick()
            }
            is HMSNotificationType.TerminalError -> {
                meetingViewModel.startMeeting()
                handleNotificationDismissClick()
            }
            is HMSNotificationType.RecordingFailedToStart -> {
                meetingViewModel.recordMeeting(true)
                handleNotificationDismissClick()
            }

            is HMSNotificationType.ScreenShare -> {
                meetingViewModel.stopScreenshare()
                handleNotificationDismissClick()
            }

            is HMSNotificationType.OpenPollOrQuiz -> {
                PollDisplayFragment.launch(type.pollId, supportFragmentManager)
                handleNotificationDismissClick()
            }

            else -> {}
        }
    }

    private fun handleNotificationDismissClick() {
        binding.notifcationCardList?.swipe()
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Check if this was a screenshare-only permission request (only POST_NOTIFICATIONS)
        val isScreensharePermissionRequest = results.size == 1 && results.containsKey(POST_NOTIFICATIONS)
        if (isScreensharePermissionRequest) {
            meetingViewModel.screenshareRequest.value = Unit
            return@registerForActivityResult
        }

        // For meeting permissions, check if critical permissions (excluding POST_NOTIFICATIONS) are granted
        // POST_NOTIFICATIONS is optional - meeting works without it, just notification won't show
        val criticalPermissions = results.filterKeys { it != POST_NOTIFICATIONS }
        if (criticalPermissions.values.all { granted -> granted }) {
            meetingViewModel.permissionGranted()
        } else {
            // Leave the meeting
            meetingViewModel.leaveMeeting(null)
            // Close our activity to return to whatever the user had before
            finish()
        }
    }

}