package live.hms.app2.ui.meeting

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import live.hms.app2.R
import live.hms.app2.databinding.ActivityMeetingBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.ROOM_DETAILS

class MeetingActivity : AppCompatActivity() {

  private var _binding: ActivityMeetingBinding? = null

  private val binding: ActivityMeetingBinding
    get() = _binding!!

  var settingsStore : SettingsStore? = null

  private val meetingViewModel: MeetingViewModel by viewModels {
    MeetingViewModelFactory(
      application,
      intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityMeetingBinding.inflate(layoutInflater)

    setContentView(binding.root)
    supportActionBar?.setDisplayShowTitleEnabled(false)
    settingsStore = SettingsStore(this)

    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    val navController = navHostFragment.navController
    val topFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
    if (settingsStore?.showPreviewBeforeJoin == true && (topFragment is MeetingFragment).not()) {
      navController?.setGraph(R.navigation.preview_nav_graph)
    } else {
      navController?.setGraph(R.navigation.meeting_nav_graph)
    }

    initViewModels()

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
      Toast.makeText(this,"Spotlight: ${it?.peer?.name}", Toast.LENGTH_SHORT).show()
    }
  }
}
