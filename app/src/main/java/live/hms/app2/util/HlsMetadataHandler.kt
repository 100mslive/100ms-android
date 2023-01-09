package live.hms.app2.util;

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.hls.HlsManifest
import live.hms.app2.model.LocalMetaDataModel
import okhttp3.internal.UTC
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class HlsMetadataHandler(
    val exoPlayer: ExoPlayer,
    val listener: (LocalMetaDataModel) -> Unit,
    val context: Context
) {
    private val META_DATA_MATCHER =
        "#EXT-X-DATERANGE:ID=\"(?<id>.*)\",START-DATE=\"(?<startDate>.*)\",DURATION=(?<duration>.*),X-100MSLIVE-PAYLOAD=\"(?<payload>.*)\""

    companion object {
        const val TAG = "HlsMetadataTAG"
    }

    var handler: Handler? = null
    private var eventRunnable: Runnable? = null
    var lastFoundTag: LocalMetaDataModel? = null

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
                        val payload = matcher.group(4).orEmpty()
                        val duration = matcher.group(3).orEmpty().toLongOrNull()?.times(1000) ?: 0
                        val startDate = matcher.group(2).orEmpty()
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                        formatter.timeZone = UTC
                        val tagStartTime = formatter.parse(startDate).time
                        lastFoundTag = LocalMetaDataModel(payload, duration)
                        lastFoundTag?.startTime = tagStartTime
                        return@lastOrNull tagStartTime <= currentAbsolutePosition
                    } catch (e: Exception) {
                    }
                }
            }
            false
        }

        lastFoundTag?.let { tag ->
            val tagStartTime = tag.startTime
            if (tagStartTime <= currentAbsolutePosition && currentAbsolutePosition - tagStartTime <= tag.duration) {
                listener.invoke(tag)
                Toast.makeText(context, tag.payload, Toast.LENGTH_LONG).show()
            }
        }
    }

}