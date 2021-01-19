package live.hms.android100ms.ui.meeting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
}
