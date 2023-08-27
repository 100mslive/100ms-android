package live.hms.roomkit.ui.meeting.videogrid

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import live.hms.roomkit.ui.meeting.MeetingViewModel

class GridViewModelFactory(
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GridViewModel::class.java)) {
            return GridViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}