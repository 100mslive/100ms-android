package live.hms.app2.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import live.hms.app2.databinding.ActivityQrCodeBinding
import live.hms.roomkit.R


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

        checkPermissionsAndLaunchRequest()

        binding.closeBtn.setOnClickListener {
            leaveActivity()
        }

        binding.btnWithLinkInstead.setOnClickListener {
            leaveActivity()
        }
    }

    private fun leaveActivity() {
        finish()
    }

    private fun init() {
        binding.barcodeScanner.apply {
            initializeFromIntent(intent)
            barcodeView.decodeContinuous {
                val intent = Intent()
                intent.putExtra(QR_INTENT_RESULT,it.result.text)
                setResult(Activity.RESULT_OK,intent)
                finish()
            }
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

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                init()
            } else {
                showDialogAboutWhyQrCodesWillNotWork()
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    private fun checkPermissionsAndLaunchRequest() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                init()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
//                showInContextUI(...)
                    showDialogAboutWhyQrCodesWillNotWork()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA)
            }
        }
    }

    private fun showDialogAboutWhyQrCodesWillNotWork() {
        val builder = AlertDialog.Builder(this)
            .setMessage("QR code scanner requires the camera permission to work.")
            .setTitle("Camera Permission Denied")
            .setCancelable(true)

//        builder.setPositiveButton(R.string.turn_on) { dialog, _ ->
//            if (event.request.track is HMSLocalAudioTrack) {
//                meetingViewModel.setLocalAudioEnabled(true)
//            } else if (event.request.track is HMSLocalVideoTrack) {
//                meetingViewModel.setLocalVideoEnabled(true)
//            }
//            dialog.dismiss()
//        }

        builder.setPositiveButton(live.hms.app2.R.string.allow_camera_permission) { dialog, _ ->
            dialog.dismiss()
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA)
        }

        builder.setNegativeButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
            leaveActivity()
        }

        builder.setOnCancelListener {
            leaveActivity()
        }

        builder.create().apply { show() }
    }
}