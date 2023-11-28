package live.hms.roomkit.ui.meeting.activespeaker

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import live.hms.roomkit.ui.meeting.activespeaker.portablehls.HmsHlsCue
import live.hms.video.utils.GsonUtils

class DisplayHlsCuesUseCase(val showText: (String) -> Unit) {

    private val listToShow = mutableListOf<String>()

    /**
     * The only reason this is a suspending function is so that it will be queued
     * and that the listToShow will never run into concurrent modification.
     */
    suspend fun addCue(hlsCue: HmsHlsCue) {
        val duration = if (hlsCue.endDate?.time == null) {
            Integer.MAX_VALUE
        } else {
            // The hls time will always be relative to playback time not current time.
            ((hlsCue.endDate?.time ?: 0) - hlsCue.startDate.time).toInt()
        }
        if (duration > 0) {
            val result = convert(hlsCue.payloadval)
            listToShow.add(result)
            showList()
            delay(duration.toLong())
            listToShow.remove(result)
            showList()
        }
    }


    private fun showList() {
        val text =
            listToShow.fold("") { displayString, currentText -> displayString + '\n' + currentText }
        showText(text)
    }

    private fun convert(text: String?): String {
        if (text == null)
            return "null"

        val isEmojiReact = text.contains("type") && text.contains("emojiId")
        if (!isEmojiReact)
            return text

        val emojiReact = GsonUtils.gson.fromJson(text, EmojiReact::class.java)
        if (emojiReact.type != "EMOJI_REACTION")
            return text

        return when (emojiReact.emojiId) {
            "+1" -> "👍"
            "-1" -> "👎"
            "wave" -> "👋"
            "clap" -> "👏"
            "fire" -> "🔥"
            "tada" -> "🎉"
            "heart_eyes" -> "😍"
            "joy" -> "😂"
            "open_mouth" -> "😮"
            "sob" -> "😭"
            else -> text
        }
    }
}

private data class EmojiReact(
    @SerializedName("type") val type: String,
    @SerializedName("emojiId") val emojiId: String
)
