package live.hms.roomkit.ui.polls.leaderboard

import java.math.RoundingMode

fun millisecondsToDisplayTime(milliseconds: Long): String {
    val minutes = milliseconds / 1000 / 60
    val seconds = milliseconds / 1000 % 60
    val secondsStr = seconds.toString()
    val secs: String = if (secondsStr.length >= 2) {
        secondsStr.substring(0, 2)
    } else {
        "0$secondsStr"
    }
    return "$minutes:$secs"
}
fun millisToText(milliseconds : Long?,
                 hyphenateEmptyValues : Boolean,
                 secondsText : String): String {
    return if (milliseconds == 0L || milliseconds == null) {
        if(hyphenateEmptyValues) "-" else ""
    }
    else {
        val hasHours = milliseconds > 1000*60*60
        val hasMinutes = milliseconds > 1000*60

        val hours = milliseconds / 1000 / (60*60)
        val minutes = (milliseconds / 1000 / 60) % 60
        val seconds = (milliseconds % 60000 )/ 1000f

        val prefix = if (hasHours) {
            "$hours" + 'h' + " $minutes" + "m"
        } else if (hasMinutes) {
                "$minutes" + "m"
        } else {
            ""
        }

        val suffix = if(hasHours || hasMinutes) // if it has hours then don't show seconds
            ""
            else  " ${seconds.toBigDecimal().setScale(2, RoundingMode.FLOOR).toDouble()} seconds"
        "$prefix$suffix"
    }
}