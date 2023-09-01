package live.hms.roomkit.ui.meeting

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.animation.RootViewDeferringInsetsCallback
import live.hms.roomkit.databinding.ActivityMeetingBinding
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.notification.CardStackLayoutManager
import live.hms.roomkit.ui.notification.CardStackListener
import live.hms.roomkit.ui.notification.Direction
import live.hms.roomkit.ui.notification.HMSNotification
import live.hms.roomkit.ui.notification.HMSNotificationAdapter
import live.hms.roomkit.ui.notification.HMSNotificationDiffCallBack
import live.hms.roomkit.ui.notification.HMSNotificationType
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.init
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        settingsStore = SettingsStore(this)

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
        val roomCode: String = intent!!.getStringExtra(ROOM_CODE)!!
        binding.progressBar.visibility = View.VISIBLE
        //todo show a loader UI
        meetingViewModel.initSdk(roomCode, hmsPrebuiltOption, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                runOnUiThread {
                    Toast.makeText(this@MeetingActivity, error.message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onSuccess() {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val navController = navHostFragment.navController
                    val topFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                    if (settingsStore?.showPreviewBeforeJoin == true && (topFragment is MeetingFragment).not()) navController?.setGraph(
                        R.navigation.meeting_nav_graph, intent.extras
                    )
                    else navController?.setGraph(R.navigation.no_preview_nav_graph, intent.extras)

                    initViewModels()
                }
            }
        })

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Permissions handling
        lifecycleScope.launch {
            meetingViewModel.events.collect { event ->
                if (event is MeetingViewModel.Event.RequestPermission) {
                    requestedPermissions = event.permissions
                    requestPermissionLauncher.launch(event.permissions)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun initViewModels() {
        meetingViewModel.isRecording.observe(this) {
            invalidateOptionsMenu()
        }
        meetingViewModel.pinnedTrack.observe(this) {
            if (it != null) Toast.makeText(this, "Spotlight: ${it.peer.name}", Toast.LENGTH_SHORT)
                .show()
        }

        meetingViewModel.hmsNotificationEvent.observe(this) {
            triggerNotification(it)
        }

    }

    private fun triggerNotification(hmsNotification: HMSNotification) {
        initNotificationUI()
        appendNotification(hmsNotification)
    }

    private fun appendNotification(hmsNotification: HMSNotification) {
        binding.notifcationCardList.visibility = View.VISIBLE
        val old = hmsNotificationAdapter.getItems()
        val new = mutableListOf<HMSNotification>().apply {
            addAll(old)
            add(notificationManager!!.topPosition, hmsNotification)

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
                meetingViewModel.requestBringOnStage(type.handRaisePeer)
                handleNotificationDismissClick()
            }
            is HMSNotificationType.TerminalError -> {
                meetingViewModel.startMeeting()
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
    ) {
        // Do not prevent joining if bluetooth connect is denied.
        if (it.filterKeys { key -> key != BLUETOOTH_CONNECT }.values.all { granted -> granted }) meetingViewModel.permissionGranted()
        else {
            // Leave the meeting
            meetingViewModel.leaveMeeting(null)
            // Close our activity to return to whatever the user had before
            finish()
        }
    }

}
