package live.hms.roomkit.ui.diagnostic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import live.hms.video.sdk.HMSSDK

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    // First create a new sdk instance
    val hmsSDK = HMSSDK.builder().build()
    // Now get the diagonistic sdk instance
// optional parameter to pass userId so we can send them the correct Data if req
    val hmsDiagnostics = hmsSDK.getDiagnosticSDK(userId: String?)
}