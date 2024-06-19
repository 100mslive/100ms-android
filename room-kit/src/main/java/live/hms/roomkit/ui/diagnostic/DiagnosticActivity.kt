package live.hms.roomkit.ui.diagnostic

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme

class DiagnosticActivity : AppCompatActivity() {

    private val viewModel: DiagnosticViewModel by viewModels {
        DiagnosticViewModelFactory(
            application,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)
        if (HMSPrebuiltTheme.getColours() == null) {
            HMSPrebuiltTheme.setTheme(HMSPrebuiltTheme.getDefaultHmsColorPalette())
        }
        viewModel.initSDK()
        setupNavGraph()

    }

    private fun setupNavGraph() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = navHostFragment?.findNavController()
        navController?.setGraph(R.navigation.diagnostic_nav_graph, intent.extras)
    }
}