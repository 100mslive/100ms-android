package live.hms.app2.ui.meeting

interface ILocalMediaControl {
    fun isLocalAudioEnabled() : Boolean?
    fun isLocalVideoEnabled() :  Boolean?
    fun setLocalAudioEnabled(enabled : Boolean)
    fun setLocalVideoEnabled(enabled : Boolean)
}