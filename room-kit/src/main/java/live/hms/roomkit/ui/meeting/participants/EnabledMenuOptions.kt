package live.hms.roomkit.ui.meeting.participants

data class EnabledMenuOptions(
    val bringOnStage : Boolean,
    val bringOffStage : Boolean,
    val lowerHand : Boolean,
    val removeParticipant : Boolean,
    val toggleMedia : Boolean,
    val audioIsOn : Boolean? = null,
    val videoIsOn : Boolean? = null
)