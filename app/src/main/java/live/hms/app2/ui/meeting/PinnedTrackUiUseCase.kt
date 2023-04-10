package live.hms.app2.ui.meeting

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * The purpose of this mediator livedata is to show global pinned tracks before local ones.
 * Locally pinned tracks will only be shown if the global one isn't present.
 */
class PinnedTrackUiUseCase(local : LiveData<MeetingTrack?>,
global : LiveData<MeetingTrack?>) : MediatorLiveData<MeetingTrack?>() {
    private val TAG = "PinnedTrackUIUseCase"
    var isLocalTrackPinned = false
        private set

    init {
        addSource(local) { localMeetingTrack ->
            // If it's a local track only set the value if the global one isn't set.
            if(global.value == null) {
                Log.d(TAG,"Setting local ${localMeetingTrack?.peer?.name}")
                this@PinnedTrackUiUseCase.value = localMeetingTrack
                isLocalTrackPinned = true
            } else {
                Log.d(TAG,"Setting local ${localMeetingTrack?.peer?.name}")
            }
        }
        addSource(global) { globalMeetingTrack ->
            Log.d(TAG,"Setting global ${globalMeetingTrack?.peer?.name}")
            this@PinnedTrackUiUseCase.value = globalMeetingTrack
            isLocalTrackPinned = false
            if(globalMeetingTrack == null) {
                Log.d(TAG,"Setting to local since global is null ${local.value?.peer?.name}")
                this@PinnedTrackUiUseCase.value = local.value
            }
        }
    }
}