package live.hms.roomkit.ui.meeting.activespeaker.portablehls


import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.hls.HlsManifest

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.regex.Pattern

data class LocalMetaDataModel(
    val payload: String,
    val duration: Long,
    var startTime: Long = 0,
    var id : String? = null
    // also needs id and end date
)

class HlsMetadataHandler(
    val exoPlayer: ExoPlayer,
    val listener: (HmsHlsCue) -> Unit
) {
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    private val META_DATA_MATCHER =
        "#EXT-X-DATERANGE:ID=\"(?<id>.*)\",START-DATE=\"(?<startDate>.*)\",DURATION=(?<duration>.*),X-100MSLIVE-PAYLOAD=\"(?<payload>.*)\""

    companion object {
        const val TAG = "HlsMetadataTAG"
    }

    var handler: Handler? = null
    private var eventRunnable: Runnable? = null

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
        eventRunnable = null
        handler = null
    }

    private fun getCurrentAbsoluteTime(exoPlayer: ExoPlayer): Long {
        val windowIndex = exoPlayer.currentMediaItemIndex
        val timeline = exoPlayer.currentTimeline
        val window = timeline.getWindow(windowIndex, Timeline.Window())
        return window.windowStartTimeMs + exoPlayer.currentPosition
    }

    private fun handle() {
        val hlsManifest = exoPlayer.currentManifest as HlsManifest?

        val currentAbsolutePosition = getCurrentAbsoluteTime(exoPlayer)

        val allTags = hlsManifest?.mediaPlaylist?.tags?.filter { !knownTags.contains(it) && it.contains("X-100MSLIVE-PAYLOAD") }

        allTags?.forEach {
            // Add it to known tags
            knownTags.add(it)
            // Get cues from the tags
            val cue = getCueFromTag(it)
            if(cue != null)
                existingCues.add(cue)
        }
        // Check all the existing cues and send them if they should be sent

        // Find any cues to show
        val cuesToShow = existingCues.filter { tag ->
            val tagStartTime = tag.startDate.time
            tagStartTime <= currentAbsolutePosition && (tag.endDate == null || currentAbsolutePosition <= tag.endDate.time)
        }
        // Remove them from existing list
        existingCues.removeAll(cuesToShow)
        // Show them all
        cuesToShow.forEach(listener)

        if(knownTags.size > 50) {
//            Log.d("TrimmingTags","All tags size: ${hlsManifest?.mediaPlaylist?.tags?.size}")
//            Log.d("TrimmingTags","Start")
            // Keep only the tags that are still under consideration.
            val currentExistingTags = hlsManifest?.mediaPlaylist?.tags?.toSet()
            val trimmedTags = currentExistingTags?.intersect(knownTags)?.toMutableSet()
            if(trimmedTags != null) {
//                Log.d("TrimmingTags","Trimming")
//                Log.d("TrimmingTags","Old: $knownTags")
                knownTags = trimmedTags
//                Log.d("TrimmingTags","New: $knownTags")
            } else {
//                Log.d("TrimmingTags","Not trimming")
            }
//            Log.d("TrimmingTags","End")
        }
    }

    private val existingCues = mutableListOf<HmsHlsCue>()
    // These are all the tags that have been processed.
    private var knownTags = mutableSetOf<String>()
    private fun getCueFromTag(tag : String?) : HmsHlsCue? {
        if(tag == null)
            return null
        if (tag.contains("EXT-X-DATERANGE")) {
            val pattern = Pattern.compile(META_DATA_MATCHER)
            val matcher = pattern.matcher(tag)
            if (matcher.matches()) {
                try {
                    val payloadJson = String(Base64.decode(matcher.group(4).orEmpty(),Base64.DEFAULT))
                    val data = JSONObject(payloadJson)
                    val payload = data.getString("payload")
                    val startDate = data.getString("start_date")//matcher.group(2).orEmpty()
                    val endDate = data.getString("end_date")
                    val tagStartTime = formatter.parse(startDate)
                    val tagEndTime = formatter.parse(endDate)
                    if(tagStartTime == null){
                        return null
                    }
                    val currentCue = HmsHlsCue(tagStartTime, tagEndTime, payload)
                    return currentCue//HlsCueContainer(tag, currentCue, false)
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
        return null
    }
}