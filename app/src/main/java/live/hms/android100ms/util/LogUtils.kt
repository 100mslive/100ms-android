package live.hms.android100ms.util

import android.os.Build
import android.util.Log


object LogUtils {
  /** Information about the current build, taken from system properties.  */
  @JvmStatic
  fun logDeviceInfo(tag: String?) {
    Log.d(
      tag, "Android SDK: " + Build.VERSION.SDK_INT + ", "
          + "Release: " + Build.VERSION.RELEASE + ", "
          + "Brand: " + Build.BRAND + ", "
          + "Device: " + Build.DEVICE + ", "
          + "Id: " + Build.ID + ", "
          + "Hardware: " + Build.HARDWARE + ", "
          + "Manufacturer: " + Build.MANUFACTURER + ", "
          + "Model: " + Build.MODEL + ", "
          + "Product: " + Build.PRODUCT
    )
  }
}
