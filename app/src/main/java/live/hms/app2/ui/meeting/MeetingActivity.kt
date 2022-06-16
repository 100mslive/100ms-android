package live.hms.app2.ui.meeting

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
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
    setSupportActionBar(binding.containerToolbar.toolbar)
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

    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      binding.containerToolbar.container.visibility = View.GONE
    } else {
      binding.containerToolbar.container.visibility = View.VISIBLE
    }

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  @SuppressLint("RestrictedApi")
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_meeting, menu)

    if (menu is MenuBuilder) {
      menu.setOptionalIconsVisible(true)
    }

    return true
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }

  private fun initViewModels() {
    meetingViewModel.title.observe(this) {
      binding.containerToolbar.toolbar.setTitle(it)
    }
    meetingViewModel.isRecording.observe(this) {
      invalidateOptionsMenu()
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    super.onPrepareOptionsMenu(menu)

    menu?.findItem(R.id.action_record)?.apply {
      when (meetingViewModel.isRecording.value) {
        RecordingState.RECORDING -> {
          // red
          isVisible = true
          this.icon.setTint(getColor(R.color.recording))
        }
        RecordingState.NOT_RECORDING_OR_STREAMING -> {
          isVisible = false
        }
        RecordingState.NOT_RECORDING_TRANSITION_IN_PROGRESS,
        RecordingState.RECORDING_TRANSITIONING_TO_NOT_RECORDING -> {
          // White
          isVisible = true
          // change the colour to transitioning
          this.icon.setTint(getColor(R.color.recording_transition))
        }
        RecordingState.STREAMING -> {
          // Blue
          isVisible = true
          this.icon.setTint(getColor(R.color.streaming))
        }
        RecordingState.STREAMING_AND_RECORDING -> {
          // Orange
          isVisible = true
          this.icon.setTint(getColor(R.color.streaming_recording))
        }
        else ->{}
      }

      setOnMenuItemClickListener {
        meetingViewModel.stopRecording()
        true
      }
    }
    return true
  }
}
