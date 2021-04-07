package live.hms.app2.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import live.hms.app2.R
import live.hms.app2.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

  private var _binding: ActivityHomeBinding? = null
  private val binding: ActivityHomeBinding
    get() = _binding!!

  private fun finishIfOngoingActiveTaskPresent() {
    if (!isTaskRoot
      && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
      && intent.action != null
      && intent.action.equals(Intent.ACTION_MAIN)
    ) {
      finish()
    }
  }

  override fun onResume() {
    super.onResume()
    finishIfOngoingActiveTaskPresent()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityHomeBinding.inflate(layoutInflater)

    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    // TODO: Enable turn screen on / FLAG_SHOW_WHEN_LOCKED
  }

  @SuppressLint("RestrictedApi")
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_home, menu)

    if (menu is MenuBuilder) {
      menu.setOptionalIconsVisible(true)
    }

    return true
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }
}