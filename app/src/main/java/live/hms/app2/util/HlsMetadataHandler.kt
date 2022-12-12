package live.hms.app2.util;

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import live.hms.app2.model.MetaDataModel
import live.hms.stats.MetaDataManager
import okhttp3.internal.UTC
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class HlsMetadataHandler(
    val exoPlayer: ExoPlayer,
    val listener: (MetaDataModel) -> Unit,
    val context: Context
) {
    private val META_DATA_MATCHER =
        "#EXT-X-DATERANGE:ID=\"(?<id>.*)\",START-DATE=\"(?<startDate>.*)\",DURATION=(?<duration>.*),X-100MSLIVE-PAYLOAD=\"(?<payload>.*)\""

    companion object {
        const val TAG = "HlsMetadataTAG"
    }

    var handler: Handler? = null
    private var eventRunnable: Runnable? = null
    var lastFoundTag: MetaDataModel? = null

    fun start() {
        eventRunnable = Runnable {
            handle()
            eventRunnable?.let { handler?.postDelayed(it, 500) }
        }

        handler = Handler(Looper.getMainLooper())
        eventRunnable?.let {
            handler?.post(it)
        }
    }

    fun stop() {
        eventRunnable?.let { handler?.removeCallbacks(it) }
    }

    private fun handle() {
        val hlsManifest = exoPlayer.currentManifest as HlsManifest?

        val windowIndex = exoPlayer.currentMediaItemIndex
        val timeline = exoPlayer.currentTimeline
        val window = timeline.getWindow(windowIndex, Timeline.Window())
        val currentAbsolutePosition = (window.windowStartTimeMs + exoPlayer.currentPosition)

        hlsManifest?.mediaPlaylist?.tags?.lastOrNull {
            if (it.contains("EXT-X-DATERANGE")) {
                val pattern = Pattern.compile(META_DATA_MATCHER)
                val matcher = pattern.matcher(it)
                if (matcher.matches()) {
                    try {
                        val payload = matcher.group(3).orEmpty()
                        val duration = matcher.group(2).orEmpty().toLongOrNull()?.times(1000) ?: 0
                        val startDate = matcher.group(1).orEmpty()
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        formatter.timeZone = UTC
                        val tagStartTime = formatter.parse(startDate).time
                        lastFoundTag = MetaDataModel(payload, duration, it)
                        lastFoundTag?.startTime = tagStartTime
                        return@lastOrNull tagStartTime <= currentAbsolutePosition
                    } catch (e: Exception) {}
                }
            }
            false
        }

        lastFoundTag?.let { tag ->
            val tagStartTime = tag.startTime
            if (tagStartTime <= currentAbsolutePosition && currentAbsolutePosition - tagStartTime <= tag.duration) {
                Toast.makeText(context, tag.payload, Toast.LENGTH_LONG).show()
            }
        }
    }

}