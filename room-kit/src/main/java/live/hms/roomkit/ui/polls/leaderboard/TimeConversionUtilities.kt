package live.hms.roomkit.ui.polls.leaderboard

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
        val minutes = milliseconds / 1000 / 60 % 60
        val seconds = milliseconds / 1000f % 60

        val prefix = if (hasHours) {
            "$hours $minutes" + "m"
        } else if (hasMinutes) {
                "$minutes" + "m"
        } else {
            ""
        }

        val suffix = if(hasHours || hasMinutes) // if it has hours then don't show seconds
            ""
            else  String.format("%.1f$secondsText", seconds)
        "$prefix$suffix"
    }
}