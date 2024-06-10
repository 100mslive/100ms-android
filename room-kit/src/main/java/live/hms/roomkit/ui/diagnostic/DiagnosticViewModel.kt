package live.hms.roomkit.ui.diagnostic

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import live.hms.video.diagnostics.HMSCameraCheckListener
import live.hms.video.error.HMSException
import live.hms.video.media.settings.HMSVideoTrackSettings
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.HMSSDK
import java.util.UUID

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    // First create a new sdk instance
    val hmsSDK = HMSSDK.Builder(application).build()
    var regionCode = "in"


    val diagnosticSDK by lazy { hmsSDK.getDiagnosticSDK(getConsistentUserIdOverSessions()) }

    fun getRegionList() = listOf(Pair("in", "India"), Pair("eu", "Europe"), Pair("us", "US"))
    fun setRegionPreference(regionName: String) {
        // Set the region preference
        getRegionList().forEach {
            if (it.second == regionName) {
                regionCode = it.first
            }
        }
    }


    val cameraTrackLiveData = MutableLiveData<HMSVideoTrack?>()
    fun cameraPermssionGranted() {
        diagnosticSDK.startCameraCheck(
            HMSVideoTrackSettings.CameraFacing.FRONT,
            object : HMSCameraCheckListener {
                override fun onError(error: HMSException) {

                }

                override fun onVideoTrack(localVideoTrack: HMSVideoTrack) {
                    cameraTrackLiveData.postValue(localVideoTrack)
                }
            })
    }

    private fun getConsistentUserIdOverSessions(): String {
        val sharedPreferences = getApplication<Application>().getSharedPreferences(
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

    fun stopCameraCheck() {
        diagnosticSDK.stopCameraCheck()
        cameraTrackLiveData.postValue(null)
    }

    fun initSDK() {

    }


}