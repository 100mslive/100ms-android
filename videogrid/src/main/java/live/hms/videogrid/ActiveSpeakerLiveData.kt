package live.hms.videogrid

import androidx.lifecycle.MediatorLiveData
import live.hms.video.sdk.reactive.MeetingTrack

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

    abstract fun updateMaxActiveSpeaker(rowCount: Int, columnCount: Int)

    //refresh is required when row or column count is changed
    fun refresh(rowCount: Int, columnCount: Int) {
        setValue(value)
        //changing the column or row span would update the current active speaker count visible on the grid
        updateMaxActiveSpeaker(rowCount, columnCount)
    }
}