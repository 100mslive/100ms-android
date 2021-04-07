package live.hms.app2.ui.meeting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import live.hms.app2.model.RoomDetails

class MeetingViewModelFactory(
  private val application: Application,
  private val roomDetails: RoomDetails
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(MeetingViewModel::class.java)) {
      return MeetingViewModel(application, roomDetails) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class $modelClass")
  }
}