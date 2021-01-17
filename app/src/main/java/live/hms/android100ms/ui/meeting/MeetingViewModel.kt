package live.hms.android100ms.ui.meeting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeetingViewModel : ViewModel() {

    companion object {
        private const val TAG = "MeetingViewModel"
    }

    val selectedOption = MutableLiveData(MeetingOptions.NONE)

    fun selectOption(option: MeetingOptions) {
        Log.v(TAG, "Selected MeetingOption=${option}")
        selectedOption.value = option
    }
}