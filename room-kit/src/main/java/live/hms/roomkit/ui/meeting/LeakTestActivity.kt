package live.hms.roomkit.ui.meeting

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import live.hms.roomkit.R
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.util.ROOM_CODE
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.TOKEN

class LeakTestActivity : AppCompatActivity() {

    private val leakTestViewModel: LeakTestViewModel by viewModels {
        LeakTestViewModelFactory(
            application,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_test)

        val hmsPrebuiltOption: HMSPrebuiltOptions? =
            intent!!.extras!![ROOM_PREBUILT] as? HMSPrebuiltOptions

        val roomCode: String = intent?.getStringExtra(ROOM_CODE) ?: ""
        val token: String = intent?.getStringExtra(TOKEN) ?: ""

        leakTestViewModel.initSdk(hmsPrebuiltOption, roomCode)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Create a WakeLock with the PARTIAL_WAKE_LOCK level
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakeLockTag")

        // Acquire the WakeLock
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)


    }

}