package live.hms.app2.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import live.hms.app2.R

class NetworkQualityHelper {

    companion object {
        fun getNetworkResource(downlinkSpeed: Int?, context: Context): Drawable? {
            return when (downlinkSpeed) {
                0 -> {
                    (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_0))
                }
                1 -> (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_2))
                2 -> (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_3))
                3 -> (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_4))
                4, 5 -> {
                    (ContextCompat.getDrawable(context, R.drawable.ic_baseline_wifi_5))
                }
                else -> {
                    null
                }
            }
        }
    }
}