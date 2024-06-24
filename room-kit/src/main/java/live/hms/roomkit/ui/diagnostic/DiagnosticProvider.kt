package live.hms.roomkit.ui.diagnostic

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import live.hms.video.diagnostics.HMSDiagnostic
import live.hms.video.sdk.HMSSDK
import java.util.UUID

class DiagnosticProvider(private val application: Application) {
    private var hms : Pair<HMSSDK,HMSDiagnostic>? = null
    private val consistentUserId = getConsistentUserIdOverSessions()
    fun disposeOfDiagnostic() {
        hms?.second?.stopConnectivityCheck()
        hms = null
    }
    fun getSdk() = hms?.first ?: createInstance(application).first
    fun getDiagnosticSdk() : HMSDiagnostic = hms?.second ?: createInstance(application).second
    private fun createInstance(application: Application) : Pair<HMSSDK, HMSDiagnostic> {
        val hmsSDK = HMSSDK.Builder(application).build()
        val diag = hmsSDK.getDiagnosticSDK(consistentUserId)
        return Pair(hmsSDK, diag).apply { hms = this }
    }

    private fun getConsistentUserIdOverSessions(): String {
        val sharedPreferences = application.getSharedPreferences(
            "your-activity-preference", Context.MODE_PRIVATE
        )
        if (sharedPreferences.getString("saved_user_id_blocklist", null) == null) {
            sharedPreferences.edit {
                putString(
                    "saved_user_id_blocklist", UUID.randomUUID().toString()
                )
            }
        }
        return sharedPreferences.getString("saved_user_id_blocklist", null).orEmpty()
    }

}