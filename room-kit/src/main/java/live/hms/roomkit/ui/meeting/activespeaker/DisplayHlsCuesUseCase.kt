package live.hms.roomkit.ui.meeting.activespeaker

/*class DisplayHlsCuesUseCase(val showText: (String) -> Unit,
    val openPoll : (pollId :String) -> Unit) {

    private val listToShow = mutableListOf<String>()

    *//**
     * The only reason this is a suspending function is so that it will be queued
     * and that the listToShow will never run into concurrent modification.
     *//*
    suspend fun addCue(hlsCue: HmsHlsCue) {
        val duration = if (hlsCue.endDate?.time == null) {
            Integer.MAX_VALUE
        } else {
            // The hls time will always be relative to playback time not current time.
            ((hlsCue.endDate?.time ?: 0) - hlsCue.startDate.time).toInt()
        }
        if (duration > 0) {
            when(val result = convert(hlsCue.payloadval)) {
                CueTypes.Invalid -> {} // do nothing
                is CueTypes.DisplayText-> {
                    listToShow.add(result.text)
                    showList()
                    delay(duration.toLong())
                    listToShow.remove(result.text)
                    showList()
                }
                // Might need to be single shot somehow.
                is CueTypes.PollId -> openPoll(result.pollId)
            }
        }
    }


    private fun showList() {
        val text =
            listToShow.fold("") { displayString, currentText -> displayString + '\n' + currentText }
        showText(text)
    }

    private fun convert(text: String?): CueTypes {
        if (text == null)
            return CueTypes.Invalid

        val isEmojiReact = text.contains("type") && text.contains("emojiId")
        val isPoll = text.startsWith(POLL_IDENTIFIER_FOR_HLS_CUE)
        if(isEmojiReact) {
            val emojiReact = GsonUtils.gson.fromJson(text, EmojiReact::class.java)
            if (emojiReact.type != "EMOJI_REACTION")
                return CueTypes.DisplayText.Text(text)

            val emoji = when (emojiReact.emojiId) {
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
                else -> null
            }
            if(emoji == null)
                CueTypes.DisplayText.Text(text)
            else CueTypes.DisplayText.Emoji(emoji)
        } else if (isPoll) {
            return CueTypes.PollId(text.drop(POLL_IDENTIFIER_FOR_HLS_CUE.length))
        }

        return CueTypes.DisplayText.Text(text)
    }
}

sealed class CueTypes {
    object Invalid :CueTypes()
    sealed class DisplayText(open val text : String) : CueTypes() {
        data class Text(override val text : String) : DisplayText(text)
        data class Emoji(override val text : String) : DisplayText(text)
    }

    data class PollId(val pollId :String) : CueTypes()
}

private data class EmojiReact(
    @SerializedName("type") val type: String,
    @SerializedName("emojiId") val emojiId: String
)*/
