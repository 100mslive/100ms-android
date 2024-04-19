package live.hms.roomkit.ui.meeting

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TranscriptionsPositionUseCase(private val scope : CoroutineScope) {
    val TAG = "TranscPositionUseCase"
    val transcriptionsPosition = MutableLiveData(MeetingViewModel.TranscriptionsPosition.BOTTOM)


    private var isScreenShare : Boolean = false
    private var isChatEnabled : Boolean = false
    private var lock = Mutex()
    fun chatStateChanged(enabled: Boolean) {
        scope.launch {
            lock.withLock {
//                Log.d(TAG, "Chatstate: $enabled")
                isChatEnabled = enabled
                transcriptionsPosition.postValue(recalculate())
            }
        }
    }

    fun setScreenShare(enabled : Boolean) {
        scope.launch {
            lock.withLock {
//                Log.d(TAG, "Screenshare: $enabled")
                isScreenShare = enabled

                transcriptionsPosition.postValue(recalculate())
            }
        }
    }

    private fun recalculate() : MeetingViewModel.TranscriptionsPosition {
        return if(!isChatEnabled)
            MeetingViewModel.TranscriptionsPosition.BOTTOM
        else if(isScreenShare)
            MeetingViewModel.TranscriptionsPosition.SCREENSHARE_TOP
        else
            MeetingViewModel.TranscriptionsPosition.TOP
//        Log.d(TAG, "recalculate $r")
    }

}