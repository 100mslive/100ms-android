package live.hms.roomkit.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import live.hms.roomkit.R

class NetworkQualityHelper {

    companion object {
        fun getNetworkResource(downlinkSpeed: Int?, context: Context): Drawable? {
            return when (downlinkSpeed) {
                0 -> (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_0))
                1 -> (ContextCompat.getDrawable(context, R.drawable.ic_signal_terrible))
                2 -> (ContextCompat.getDrawable(context, R.drawable.ic_signal_weak))
                3 -> (ContextCompat.getDrawable(context, R.drawable.ic_signal_medium))
                4, 5 -> {
                    (ContextCompat.getDrawable(context, R.drawable.ic_signal_strong))
                }
                else -> {
                    null
                }
            }
        }
    }
}