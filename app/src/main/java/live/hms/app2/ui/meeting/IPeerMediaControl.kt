package live.hms.app2.ui.meeting

interface IPeerMediaControl {
    fun isPeerAudioEnabled() : Boolean?
    fun setPeerAudioEnabled(enabled : Boolean)
}