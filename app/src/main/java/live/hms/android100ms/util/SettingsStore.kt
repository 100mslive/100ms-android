package live.hms.android100ms.util

import android.content.Context
import androidx.core.content.edit

class SettingsStore(private val context: Context) {

    companion object {
        const val SETTINGS_SHARED_PREF = "settings-shared-preferences"
        const val PUBLISH_VIDEO = "publish-video"
        const val PUBLISH_AUDIO = "publish-audio"
        const val VIDEO_RESOLUTION = "video-resolution"
        const val CODEC = "codec"
        const val VIDEO_BITRATE = "video-bitrate"
        const val VIDEO_FRAME_RATE = "video-frame-rate"
    }


    private val sharedPreferences = context.getSharedPreferences(
        SETTINGS_SHARED_PREF, Context.MODE_PRIVATE
    )

    private fun putString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value)
            commit()
        }
    }

    private fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
            commit()
        }
    }

    public var publishVideo: Boolean
        get() = sharedPreferences.getBoolean(PUBLISH_VIDEO, true)
        set(value) = putBoolean(PUBLISH_VIDEO, value)

    public var publishAudio: Boolean
        get() = sharedPreferences.getBoolean(PUBLISH_AUDIO, true)
        set(value) = putBoolean(PUBLISH_AUDIO, value)

    public var videoResolution: String
        get() = sharedPreferences.getString(VIDEO_RESOLUTION, "640 x 480")!!
        set(value) = putString(VIDEO_RESOLUTION, value)

    public var codec: String
        get() = sharedPreferences.getString(CODEC, "VP8")!!
        set(value) = putString(CODEC, value)

    public var videoBitrate: String
        get() = sharedPreferences.getString(VIDEO_BITRATE, "256")!!
        set(value) = putString(VIDEO_BITRATE, value)

    public var videoFrameRate: String
        get() = sharedPreferences.getString(VIDEO_FRAME_RATE, "256")!!
        set(value) = putString(VIDEO_FRAME_RATE, value)
}