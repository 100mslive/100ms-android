package live.hms.app2.ui.meeting.chat

sealed class Recipient {
    object Everyone : Recipient() {
        override fun toString(): String = "Everyone"
    }
    data class Role(val name : String) : Recipient() {
        override fun toString(): String =
            name
    }
    data class Peer(val peerId : String, val name : String) : Recipient() {
        override fun toString(): String = name
    }
}