package live.hms.android100ms.ui.home

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import live.hms.android100ms.R
import live.hms.android100ms.databinding.ActivityHomeBinding
import live.hms.android100ms.util.viewLifecycle

class HomeActivity : AppCompatActivity() {

  private val binding by viewLifecycle(ActivityHomeBinding::inflate)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    // TODO: Enable turn screen on / FLAG_SHOW_WHEN_LOCKED
  }
}