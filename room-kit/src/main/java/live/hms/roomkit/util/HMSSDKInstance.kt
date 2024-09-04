package live.hms.roomkit.util

import android.app.Application
import live.hms.roomkit.ui.settings.SettingsFragment
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.video.events.AgentType
import live.hms.video.media.settings.HMSAudioTrackSettings
import live.hms.video.media.settings.HMSLogSettings
import live.hms.video.media.settings.HMSTrackSettings
import live.hms.video.media.settings.HMSVideoTrackSettings
import live.hms.video.media.settings.PhoneCallState
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.FrameworkInfo
import live.hms.video.services.LogAlarmManager

object HMSSDKInstance {

    var hmsSDK : HMSSDK? = null
    fun getSDKInstance(context: Application) : HMSSDK {
        val hmsLogSettings: HMSLogSettings =
            HMSLogSettings(LogAlarmManager.DEFAULT_DIR_SIZE, true)

        val settings = SettingsStore(context)

        val hmsTrackSettings = HMSTrackSettings.Builder()
            .audio(
                HMSAudioTrackSettings.Builder()
                    .setUseHardwareAcousticEchoCanceler(settings.enableHardwareAEC)
                    .initialState(HMSTrackSettings.InitState.UNMUTED)
                    .enableNoiseSupression(settings.enableWebrtcNoiseSuppression)
                    .enableNoiseCancellation(settings.enableKrispNoiseCancellation)
                    .setDisableInternalAudioManager(settings.detectDominantSpeaker.not())
                    .setPhoneCallMuteState(if (settings.muteLocalAudioOnPhoneRing) PhoneCallState.ENABLE_MUTE_ON_PHONE_CALL_RING else PhoneCallState.DISABLE_MUTE_ON_VOIP_PHONE_CALL_RING)
                    .build()
            )
            .video(
                HMSVideoTrackSettings.Builder().disableAutoResize(settings.disableAutoResize)
                    .forceSoftwareDecoder(settings.forceSoftwareDecoder)
                    .setDegradationPreference(settings.degradationPreferences)
                    .initialState(HMSTrackSettings.InitState.UNMUTED)
                    .cameraFacing(getVideoCameraFacing(settings))
                    .build()
            )
            .build()

        if(hmsSDK == null) {
            hmsSDK = HMSSDK
                .Builder(context)
                .haltPreviewJoinForPermissionsRequest(true)
                .setFrameworkInfo(FrameworkInfo(framework = AgentType.ANDROID_NATIVE, isPrebuilt = true))
                .setTrackSettings(hmsTrackSettings) // SDK uses HW echo cancellation, if nothing is set in builder
                .setLogSettings(hmsLogSettings)
                .build()
        }

        return hmsSDK!!
    }

    private fun getVideoCameraFacing(settings : SettingsStore) =
        if (settings.camera.contains(SettingsFragment.REAR_FACING_CAMERA)) HMSVideoTrackSettings.CameraFacing.BACK else HMSVideoTrackSettings.CameraFacing.FRONT

}