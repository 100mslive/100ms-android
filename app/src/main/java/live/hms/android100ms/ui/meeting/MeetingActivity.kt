package live.hms.android100ms.ui.meeting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import live.hms.android100ms.R
import live.hms.android100ms.databinding.ActivityMeetingBinding
import live.hms.android100ms.util.viewLifecycle

class MeetingActivity : AppCompatActivity() {

  private val binding by viewLifecycle(ActivityMeetingBinding::inflate)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)
  }

  @SuppressLint("RestrictedApi")
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_meeting, menu)

    if (menu is MenuBuilder) {
      menu.setOptionalIconsVisible(true)
    }

    return true
  }
}
