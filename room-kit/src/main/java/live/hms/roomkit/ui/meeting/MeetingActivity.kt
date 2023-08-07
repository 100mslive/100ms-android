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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.animation.RootViewDeferringInsetsCallback
import live.hms.roomkit.databinding.ActivityMeetingBinding
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener

class MeetingActivity : AppCompatActivity() {

  var requestedPermissions : Array<String> = arrayOf()
  private var _binding: ActivityMeetingBinding? = null

  private val binding: ActivityMeetingBinding
    get() = _binding!!

  var settingsStore : SettingsStore? = null

  private val meetingViewModel: MeetingViewModel by viewModels {
    MeetingViewModelFactory(
      application,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityMeetingBinding.inflate(layoutInflater)
    WindowCompat.setDecorFitsSystemWindows(window, false)



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


    val hmsPrebuiltOption : HMSPrebuiltOptions? = intent!!.extras!![ROOM_PREBUILT] as? HMSPrebuiltOptions
    val roomCode : String = intent!!.getStringExtra(ROOM_CODE)!!
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
          val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
          val navController = navHostFragment.navController
          val topFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
          if (settingsStore?.showPreviewBeforeJoin == true && (topFragment is MeetingFragment).not())
            navController?.setGraph(R.navigation.meeting_nav_graph, intent.extras)
          else
            navController?.setGraph(R.navigation.no_preview_nav_graph, intent.extras)

          initViewModels()
        }
      }
    })

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Permissions handling
    lifecycleScope.launch {
        meetingViewModel.events.collect {event ->
            if(event is MeetingViewModel.Event.RequestPermission) {
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
    meetingViewModel.title.observe(this) {
    }
    meetingViewModel.isRecording.observe(this) {
      invalidateOptionsMenu()
    }
    meetingViewModel.pinnedTrack.observe(this) {
      if(it != null)
        Toast.makeText(this,"Spotlight: ${it.peer.name}", Toast.LENGTH_SHORT).show()
    }
  }

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) {
    // Do not prevent joining if bluetooth connect is denied.
    if(it.filterKeys { key -> key != BLUETOOTH_CONNECT }.values.all { granted -> granted })
      meetingViewModel.permissionGranted()
    else {
      // Leave the meeting
      meetingViewModel.leaveMeeting(null)
      // Close our activity to return to whatever the user had before
      finish()
    }
  }

}
