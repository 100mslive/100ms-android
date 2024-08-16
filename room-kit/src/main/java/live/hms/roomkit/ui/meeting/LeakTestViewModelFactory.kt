package live.hms.roomkit.ui.meeting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider



class LeakTestViewModelFactory(
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeakTestViewModel::class.java)) {
            return LeakTestViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}