package live.hms.android100ms.ui.settings

import android.content.Context
import androidx.core.content.edit

class SettingsStore(context: Context) {

  companion object {
    const val TAG = "SettingsStore"

    const val SETTINGS_SHARED_PREF = "settings-shared-preferences"
    const val PUBLISH_VIDEO = "publish-video"
    const val CAMERA = "camera"
    const val PUBLISH_AUDIO = "publish-audio"
    const val VIDEO_RESOLUTION_WIDTH = "video-resolution-width"
    const val VIDEO_RESOLUTION_HEIGHT = "video-resolution-height"
    const val CODEC = "codec"
    const val VIDEO_BITRATE = "video-bitrate"
    const val VIDEO_FRAME_RATE = "video-frame-rate"
    const val USERNAME = "username"

    const val DETECT_DOMINANT_SPEAKER = "detect-dominant-speaker"
    const val AUDIO_POLL_INTERVAL = "audio-poll-interval"
    const val SILENCE_AUDIO_LEVEL_THRESHOLD = "silence-audio-level-threshold"

    const val LAST_USED_ROOM_ID = "last-used-room-id"
    const val ENVIRONMENT = "last-used-env"

    const val VIDEO_GRID_ROWS = "video-grid-rows"
    const val VIDEO_GRID_COLUMNS = "video-grid-columns"

    const val LEAK_CANARY = "leak-canary"
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

  private fun putLong(key: String, value: Long) {
    sharedPreferences.edit {
      putLong(key, value)
      commit()
    }
  }

  private fun putInt(key: String, value: Int) {
    sharedPreferences.edit {
      putInt(key, value)
      commit()
    }
  }

  private fun putBoolean(key: String, value: Boolean) {
    sharedPreferences.edit {
      putBoolean(key, value)
      commit()
    }
  }

  private fun putFloat(key: String, value: Float) {
    sharedPreferences.edit {
      putFloat(key, value)
      commit()
    }
  }

  var publishVideo: Boolean
    get() = sharedPreferences.getBoolean(PUBLISH_VIDEO, true)
    set(value) = putBoolean(PUBLISH_VIDEO, value)

  var camera: String
    get() = sharedPreferences.getString(CAMERA, "user")!!
    set(value) = putString(CAMERA, value)


  var publishAudio: Boolean
    get() = sharedPreferences.getBoolean(PUBLISH_AUDIO, true)
    set(value) = putBoolean(PUBLISH_AUDIO, value)

  var videoResolutionWidth: Int
    get() = sharedPreferences.getInt(VIDEO_RESOLUTION_WIDTH, 640)
    set(value) = putInt(VIDEO_RESOLUTION_WIDTH, value)

  var videoResolutionHeight: Int
    get() = sharedPreferences.getInt(VIDEO_RESOLUTION_HEIGHT, 480)
    set(value) = putInt(VIDEO_RESOLUTION_HEIGHT, value)

  var codec: String
    get() = sharedPreferences.getString(CODEC, "VP8")!!
    set(value) = putString(CODEC, value)

  var videoBitrate: Int
    get() = sharedPreferences.getInt(VIDEO_BITRATE, 256)
    set(value) = putInt(VIDEO_BITRATE, value)

  var videoFrameRate: Int
    get() = sharedPreferences.getInt(VIDEO_FRAME_RATE, 24)
    set(value) = putInt(VIDEO_FRAME_RATE, value)

  var username: String
    get() = sharedPreferences.getString(USERNAME, "Android User")!!
    set(value) = putString(USERNAME, value)

  var detectDominantSpeaker: Boolean
    get() = sharedPreferences.getBoolean(DETECT_DOMINANT_SPEAKER, true)
    set(value) = putBoolean(DETECT_DOMINANT_SPEAKER, value)

  var audioPollInterval: Long
    get() = sharedPreferences.getLong(AUDIO_POLL_INTERVAL, 1000)
    set(value) = putLong(AUDIO_POLL_INTERVAL, value)

  var silenceAudioLevelThreshold: Float
    get() = sharedPreferences.getFloat(SILENCE_AUDIO_LEVEL_THRESHOLD, 0.01f)
    set(value) = putFloat(SILENCE_AUDIO_LEVEL_THRESHOLD, value)

  var lastUsedRoomId: String
    get() = sharedPreferences.getString(LAST_USED_ROOM_ID, "")!!
    set(value) = putString(LAST_USED_ROOM_ID, value)

  var environment: String
    get() = sharedPreferences.getString(ENVIRONMENT, "prod-in")!!
    set(value) = putString(ENVIRONMENT, value)


  var videoGridRows: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_ROWS, 2)
    set(value) = putInt(VIDEO_GRID_ROWS, value)

  var videoGridColumns: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_COLUMNS, 2)
    set(value) = putInt(VIDEO_GRID_COLUMNS, value)

  var isLeakCanaryEnabled: Boolean
    get() = sharedPreferences.getBoolean(LEAK_CANARY, true)
    set(value) = putBoolean(LEAK_CANARY, value)

  inner class MultiCommitHelper {

    private val editor = sharedPreferences.edit()

    fun setPublishVideo(value: Boolean): MultiCommitHelper {
      editor.putBoolean(PUBLISH_VIDEO, value)
      return this
    }

    fun setCamera(value: String): MultiCommitHelper {
      editor.putString(CAMERA, value)
      return this
    }

    fun setPublishAudio(value: Boolean): MultiCommitHelper {
      editor.putBoolean(PUBLISH_AUDIO, value)
      return this
    }

    fun setVideoResolutionWidth(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_RESOLUTION_WIDTH, value)
      return this
    }

    fun setVideoResolutionHeight(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_RESOLUTION_HEIGHT, value)
      return this
    }

    fun setCodec(value: String): MultiCommitHelper {
      editor.putString(CODEC, value)
      return this
    }

    fun setVideoBitrate(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_BITRATE, value)
      return this
    }

    fun setVideoFrameRate(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_FRAME_RATE, value)
      return this
    }

    fun setUsername(value: String): MultiCommitHelper {
      editor.putString(USERNAME, value)
      return this
    }

    fun setDetectDominantSpeaker(value: Boolean): MultiCommitHelper {
      editor.putBoolean(DETECT_DOMINANT_SPEAKER, value)
      return this
    }

    fun setAudioPollInterval(value: Long): MultiCommitHelper {
      editor.putLong(AUDIO_POLL_INTERVAL, value)
      return this
    }

    fun setSilenceAudioLevelThreshold(value: Float): MultiCommitHelper {
      editor.putFloat(SILENCE_AUDIO_LEVEL_THRESHOLD, value)
      return this
    }

    fun setLastUsedRoomId(value: String): MultiCommitHelper {
      editor.putString(LAST_USED_ROOM_ID, value)
      return this
    }

    fun setEnvironment(value: String): MultiCommitHelper {
      editor.putString(ENVIRONMENT, value)
      return this
    }

    fun setVideoGridRows(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_GRID_ROWS, value)
      return this
    }

    fun setVideoGridColumns(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_GRID_COLUMNS, value)
      return this
    }

    fun setIsLeakCanaryEnabled(value: Boolean): MultiCommitHelper {
      editor.putBoolean(LEAK_CANARY, value)
      return this
    }

    fun commit() {
      editor.commit()
    }

  }

}