package live.hms.roomkit.ui.diagnostic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory

class DiagnosticActivity : AppCompatActivity() {

    private val meetingViewModel: DiagnosticViewModelFactory = DiagnosticViewModelFactory(application)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)
    }
}