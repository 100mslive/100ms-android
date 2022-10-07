package live.hms.app2.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import live.hms.app2.databinding.ActivityQrCodeBinding


class QrCodeActivity : AppCompatActivity() {

    private var _binding: ActivityQrCodeBinding? = null
    private val binding: ActivityQrCodeBinding
        get() = _binding!!

    companion object{
        const val QR_INTENT_RESULT = "QR_INTENT_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityQrCodeBinding.inflate(layoutInflater)
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        actionBar?.hide()
        supportActionBar?.hide()
        setContentView(binding.root)

        binding.barcodeScanner.apply {
            initializeFromIntent(intent)
            barcodeView.decodeContinuous {
                val intent = Intent()
                intent.putExtra(QR_INTENT_RESULT,it.result.text)
                setResult(Activity.RESULT_OK,intent)
                finish()
            }
        }

        binding.closeBtn.setOnClickListener {
            finish()
        }

        binding.btnWithLinkInstead.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }


}