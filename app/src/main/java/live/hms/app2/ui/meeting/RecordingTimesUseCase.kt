package live.hms.app2.ui.meeting

import java.text.SimpleDateFormat
import java.util.*

class RecordingTimesUseCase() {
    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

    fun convertTimes(startedAt : Long?, stoppedAt: Long?) : Pair<String, String> {
        val startedAt = if(startedAt == null)
            "Empty"
        else
            dateFormat.format(startedAt)

        val stoppedAt = if(stoppedAt == null)
            "Empty"
        else
            dateFormat.format(stoppedAt)

        return Pair(startedAt, stoppedAt)
    }

}