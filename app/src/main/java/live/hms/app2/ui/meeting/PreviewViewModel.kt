package live.hms.app2.ui.meeting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import live.hms.video.sdk.models.HMSRoom

class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val roomState : MutableLiveData<HMSRoom> = MutableLiveData()
    val roomStateLiveData : LiveData<HMSRoom> = roomState

    fun updateRoomState(roomState : HMSRoom) {
        this.roomState.value = roomState
    }
}