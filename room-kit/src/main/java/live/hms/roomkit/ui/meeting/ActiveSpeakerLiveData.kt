package live.hms.roomkit.ui.meeting

import androidx.lifecycle.MediatorLiveData

abstract class ActiveSpeakerLiveData : MediatorLiveData<List<MeetingTrack>>() {

    private var enableSorting = true

    fun enableSorting(enable: Boolean) {
        if (enableSorting == enable)
            return
        if (enable)
            addSpeakerSource()
        else
            removeSpeakerSource()
        enableSorting = enable
    }

    abstract fun addSpeakerSource()
    abstract fun removeSpeakerSource()

    //refresh is required when row or column count is changed
    fun refresh() {
        setValue(value)
    }
}