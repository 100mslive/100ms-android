package live.hms.app2.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import live.hms.app2.BuildConfig
import live.hms.app2.ui.meeting.MeetingViewMode
import live.hms.video.utils.HMSLogger

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
    const val SHOW_NETWORK_INFO = "show-network-info"
    const val AUDIO_POLL_INTERVAL = "audio-poll-interval"
    const val SILENCE_AUDIO_LEVEL_THRESHOLD = "silence-audio-level-threshold"

    const val LAST_USED_MEETING_URL = "last-used-meeting-url"
    const val ENVIRONMENT = "last-used-env"

    const val VIDEO_GRID_ROWS = "video-grid-rows"
    const val VIDEO_GRID_COLUMNS = "video-grid-columns"

    const val MEETING_MODE = "meeting-view-mode"
    const val LOG_LEVEL_WEBRTC = "log-level-webrtc"
    const val LOG_LEVEL_100MS_SDK = "log-level-100ms"

    const val LEAK_CANARY = "leak-canary"
    const val SHOW_RECONNECTING_PROGRESS_BARS = "show-reconnecting-progress-bar"
    const val SHOW_PREVIEW_BEFORE_JOIN = "show-preview-before-join"
    const val SUBSCRIBE_DEGRADATION = "subscribe-degradation-enabling"
    const val RTMP_URL_LIST = "rtmp-url-list"
    const val USE_HARDWARE_AEC = "use-hardware-aec"
    const val SHOW_STATS = "show-video-stats"

    val APPLY_CONSTRAINTS_KEYS = arrayOf(
      VIDEO_FRAME_RATE,
      VIDEO_BITRATE,
      VIDEO_RESOLUTION_HEIGHT,
      VIDEO_RESOLUTION_WIDTH
    )
  }

  private val sharedPreferences = context.getSharedPreferences(
    SETTINGS_SHARED_PREF, Context.MODE_PRIVATE
  )

  fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
  }

  fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
  }

  private fun putStringSet(key: String, value: Set<String>) {
    sharedPreferences.edit {
      putStringSet(key, value)
      apply()
    }
  }

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

  var enableSubscribeDegradation: Boolean
    get() = sharedPreferences.getBoolean(SUBSCRIBE_DEGRADATION, true)
    set(value) = putBoolean(SUBSCRIBE_DEGRADATION, value)

  // Set to true to enable Hardware echo cancellation else set to false to enable SW based
  var enableHardwareAEC: Boolean
    get() = sharedPreferences.getBoolean(USE_HARDWARE_AEC, true)
    set(value) = putBoolean(USE_HARDWARE_AEC, value)

  var showStats: Boolean
    get() = sharedPreferences.getBoolean(SHOW_STATS, true)
    set(value) = putBoolean(SHOW_STATS, value)

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
    get() = sharedPreferences.getString(USERNAME, "Android " + Build.MODEL)!!
    set(value) = putString(USERNAME, value)

  var detectDominantSpeaker: Boolean
    get() = sharedPreferences.getBoolean(DETECT_DOMINANT_SPEAKER, true)
    set(value) = putBoolean(DETECT_DOMINANT_SPEAKER, value)

  var audioPollInterval: Long
    get() = sharedPreferences.getLong(AUDIO_POLL_INTERVAL, 1000)
    set(value) = putLong(AUDIO_POLL_INTERVAL, value)

  var silenceAudioLevelThreshold: Int
    get() = sharedPreferences.getInt(SILENCE_AUDIO_LEVEL_THRESHOLD, 10)
    set(value) = putInt(SILENCE_AUDIO_LEVEL_THRESHOLD, value)

  var showNetworkInfo: Boolean
    get() = sharedPreferences.getBoolean(SHOW_NETWORK_INFO, true)
    set(value) = putBoolean(SHOW_NETWORK_INFO, value)

  var lastUsedMeetingUrl: String
    get() = sharedPreferences.getString(LAST_USED_MEETING_URL, "")!!
    set(value) = putString(LAST_USED_MEETING_URL, value)

  var environment: String
    get() = sharedPreferences.getString(ENVIRONMENT, "prod-init")!!
    set(value) = putString(ENVIRONMENT, value)


  var videoGridRows: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_ROWS, 2)
    set(value) = putInt(VIDEO_GRID_ROWS, value)

  var videoGridColumns: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_COLUMNS, 2)
    set(value) = putInt(VIDEO_GRID_COLUMNS, value)

  var isLeakCanaryEnabled: Boolean
    get() = sharedPreferences.getBoolean(LEAK_CANARY, false)
    set(value) = putBoolean(LEAK_CANARY, value)

  var showReconnectingProgressBars: Boolean
    get() = sharedPreferences.getBoolean(SHOW_RECONNECTING_PROGRESS_BARS, true)
    set(value) = putBoolean(SHOW_RECONNECTING_PROGRESS_BARS, value)

  var showPreviewBeforeJoin: Boolean
    get() = sharedPreferences.getBoolean(SHOW_PREVIEW_BEFORE_JOIN, true)
    set(value) = putBoolean(SHOW_PREVIEW_BEFORE_JOIN, value)

  var meetingMode: MeetingViewMode
    get() {
      val str = sharedPreferences.getString(
        MEETING_MODE,
        MeetingViewMode.ACTIVE_SPEAKER.toString()
      )!!
      return MeetingViewMode::class.nestedClasses.find { it.simpleName == str }?.objectInstance as MeetingViewMode? ?: MeetingViewMode.ACTIVE_SPEAKER
    }
    set(value) = putString(MEETING_MODE, value.toString())

  var logLevelWebrtc: HMSLogger.LogLevel
    get() {
      val str = sharedPreferences.getString(
        LOG_LEVEL_WEBRTC,
        HMSLogger.LogLevel.WARN.toString()
      )!!
      return HMSLogger.LogLevel.valueOf(str)
    }
    set(value) = putString(LOG_LEVEL_WEBRTC, value.toString())

  var logLevel100msSdk: HMSLogger.LogLevel
    get() {
      val str = sharedPreferences.getString(
        LOG_LEVEL_100MS_SDK,
        HMSLogger.LogLevel.VERBOSE.toString()
      )!!
      return HMSLogger.LogLevel.valueOf(str)
    }
    set(value) = putString(LOG_LEVEL_100MS_SDK, value.toString())

  var rtmpUrlsList: Set<String>
    get() = sharedPreferences.getStringSet(
      RTMP_URL_LIST,
      if (BuildConfig.RTMP_INJEST_URL.isEmpty()) emptySet<String>() else setOf(BuildConfig.RTMP_INJEST_URL)
    )?.toSet() ?: emptySet()
    set(value) = putStringSet(RTMP_URL_LIST, value)


  inner class MultiCommitHelper {

    private val editor = sharedPreferences.edit()

    fun setPublishVideo(value: Boolean) = apply { editor.putBoolean(PUBLISH_VIDEO, value) }
    fun setCamera(value: String) = apply { editor.putString(CAMERA, value) }
    fun setPublishAudio(value: Boolean) = apply { editor.putBoolean(PUBLISH_AUDIO, value) }
    fun setVideoResolutionWidth(value: Int) = apply { editor.putInt(VIDEO_RESOLUTION_WIDTH, value) }
    fun setVideoResolutionHeight(value: Int) =
      apply { editor.putInt(VIDEO_RESOLUTION_HEIGHT, value) }

    fun setCodec(value: String) = apply { editor.putString(CODEC, value) }
    fun setVideoBitrate(value: Int) = apply { editor.putInt(VIDEO_BITRATE, value) }
    fun setVideoFrameRate(value: Int) = apply { editor.putInt(VIDEO_FRAME_RATE, value) }
    fun setUsername(value: String) = apply { editor.putString(USERNAME, value) }
    fun setDetectDominantSpeaker(value: Boolean) =
      apply { editor.putBoolean(DETECT_DOMINANT_SPEAKER, value) }

    fun setShowNetworkInfo(value: Boolean) = apply { editor.putBoolean(SHOW_NETWORK_INFO, value) }
    fun setReconnectingShowProgressBars(value: Boolean) =
      apply { editor.putBoolean(SHOW_RECONNECTING_PROGRESS_BARS, value) }

    fun setShowPreviewBeforeJoin(value: Boolean) =
      apply { editor.putBoolean(SHOW_PREVIEW_BEFORE_JOIN, value) }

    fun setAudioPollInterval(value: Long) = apply { editor.putLong(AUDIO_POLL_INTERVAL, value) }
    fun setSilenceAudioLevelThreshold(value: Int) =
      apply { editor.putInt(SILENCE_AUDIO_LEVEL_THRESHOLD, value) }

    fun setLastUsedMeetingUrl(value: String) = apply { editor.putString(LAST_USED_MEETING_URL, value) }
    fun setEnvironment(value: String) = apply { editor.putString(ENVIRONMENT, value) }
    fun setVideoGridRows(value: Int) = apply { editor.putInt(VIDEO_GRID_ROWS, value) }
    fun setVideoGridColumns(value: Int) = apply { editor.putInt(VIDEO_GRID_COLUMNS, value) }
    fun setIsLeakCanaryEnabled(value: Boolean) = apply { editor.putBoolean(LEAK_CANARY, value) }
    fun setMeetingMode(value: String) = apply { editor.putString(MEETING_MODE, value) }
    fun setLogLevelWebRtc(value: String) = apply { editor.putString(LOG_LEVEL_WEBRTC, value) }
    fun setLogLevelWebRtc(value: HMSLogger.LogLevel) =
      apply { editor.putString(LOG_LEVEL_WEBRTC, value.toString()) }

    fun setLogLevel100msSdk(value: String) = apply { editor.putString(LOG_LEVEL_100MS_SDK, value) }
    fun setLogLevel100msSdk(value: HMSLogger.LogLevel) =
      apply { editor.putString(LOG_LEVEL_100MS_SDK, value.toString()) }

    fun setSubscribeDegradation(value: Boolean) = apply { editor.putBoolean(SUBSCRIBE_DEGRADATION, value) }
    fun setUseHardwareAEC(value: Boolean) = apply { editor.putBoolean(USE_HARDWARE_AEC, value) }
    fun setShowStats(value: Boolean) = apply { editor.putBoolean(SHOW_STATS, value) }


    fun commit() {
      editor.commit()
    }

  }

}