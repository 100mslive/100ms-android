package live.hms.app2.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import live.hms.app2.R
import live.hms.app2.databinding.ActivityHomeBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class HomeActivity : AppCompatActivity() {

  private var _binding: ActivityHomeBinding? = null
  private val binding: ActivityHomeBinding
    get() = _binding!!
  var meetingUrl : String = ""

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
    copyWeightsAssetsToDirectory()

    // TODO: Enable turn screen on / FLAG_SHOW_WHEN_LOCKED
  }

  private fun copyWeightsAssetsToDirectory(targetDirectory: String = getExternalFilesDir(null)!!.absolutePath) {
    try {
      val assetManager = assets
      val files = arrayOf(
        "lyra_config.binarypb", "lyragan.tflite",
        "quantizer.tflite", "soundstream_encoder.tflite"
      )
      val buffer = ByteArray(1024)
      var amountRead: Int
      for (file in files) {
        val inputStream: InputStream = assetManager.open(file)
        val outputFile = File(targetDirectory, file)
        val outputStream: OutputStream = FileOutputStream(outputFile)
        Log.i("HomeAct", "copying asset to " + outputFile.getPath())
        while (inputStream.read(buffer).also { amountRead = it } != -1) {
          outputStream.write(buffer, 0, amountRead)
        }
        inputStream.close()
        outputStream.close()
      }
    } catch (e: Exception) {
      Log.e("HomeAct", "Error copying assets", e)
    }
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