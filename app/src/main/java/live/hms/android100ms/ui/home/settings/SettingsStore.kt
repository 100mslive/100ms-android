package live.hms.android100ms.ui.home.settings

import android.content.Context
import androidx.core.content.edit

class SettingsStore(context: Context) {

  companion object {
    const val SETTINGS_SHARED_PREF = "settings-shared-preferences"
    const val PUBLISH_VIDEO = "publish-video"
    const val PUBLISH_AUDIO = "publish-audio"
    const val VIDEO_RESOLUTION = "video-resolution"
    const val CODEC = "codec"
    const val VIDEO_BITRATE = "video-bitrate"
    const val VIDEO_FRAME_RATE = "video-frame-rate"
    const val USERNAME = "username"

    const val LAST_USED_ROOM_ID = "last-used-room-id"
    const val LAST_USED_ENV = "last-used-env"

    const val VIDEO_GRID_ROWS = "video-grid-rows"
    const val VIDEO_GRID_COLUMNS = "video-grid-columns"
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

  private fun putInt(key: String, value: Int) {
    sharedPreferences.edit() {
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

  public var videoBitrate: Int
    get() = sharedPreferences.getInt(VIDEO_BITRATE, 256)!!
    set(value) = putInt(VIDEO_BITRATE, value)

  public var videoFrameRate: Int
    get() = sharedPreferences.getInt(VIDEO_FRAME_RATE, 24)!!
    set(value) = putInt(VIDEO_FRAME_RATE, value)

  public var username: String
    get() = sharedPreferences.getString(USERNAME, "")!!
    set(value) = putString(USERNAME, value)


  public var lastUsedRoomId: String
    get() = sharedPreferences.getString(LAST_USED_ROOM_ID, "")!!
    set(value) = putString(LAST_USED_ROOM_ID, value)

  public var lastUsedEnv: String
    get() = sharedPreferences.getString(LAST_USED_ENV, "")!!
    set(value) = putString(LAST_USED_ENV, value)


  public var videoGridRows: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_ROWS, 2)
    set(value) = putInt(VIDEO_GRID_ROWS, value)

  public var videoGridColumns: Int
    get() = sharedPreferences.getInt(VIDEO_GRID_COLUMNS, 2)
    set(value) = putInt(VIDEO_GRID_COLUMNS, value)


  inner class MultiCommitHelper {

    private val editor = sharedPreferences.edit()

    public fun setPublishVideo(value: Boolean): MultiCommitHelper {
      editor.putBoolean(PUBLISH_VIDEO, value)
      return this
    }

    public fun setPublishAudio(value: Boolean): MultiCommitHelper {
      editor.putBoolean(PUBLISH_AUDIO, value)
      return this
    }

    public fun setVideoResolution(value: String): MultiCommitHelper {
      editor.putString(VIDEO_RESOLUTION, value)
      return this
    }

    public fun setCodec(value: String): MultiCommitHelper {
      editor.putString(CODEC, value)
      return this
    }

    public fun setVideoBitrate(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_BITRATE, value)
      return this
    }

    public fun setVideoFrameRate(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_FRAME_RATE, value)
      return this
    }

    public fun setUsername(value: String): MultiCommitHelper {
      editor.putString(USERNAME, value)
      return this
    }


    public fun setLastUsedRoomId(value: String): MultiCommitHelper {
      editor.putString(LAST_USED_ROOM_ID, value)
      return this
    }

    public fun setLastUsedEnv(value: String): MultiCommitHelper {
      editor.putString(LAST_USED_ENV, value)
      return this
    }

    public fun setVideoGridRows(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_GRID_ROWS, value)
      return this
    }

    public fun setVideoGridColumns(value: Int): MultiCommitHelper {
      editor.putInt(VIDEO_GRID_COLUMNS, value)
      return this
    }

    public fun commit() {
      editor.commit()
    }

  }

}