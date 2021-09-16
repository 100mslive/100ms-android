package live.hms.app2.util;

import android.view.View
import live.hms.app2.BuildConfig


/**
 * Helper method which throws an exception  when an assertion has failed.
 */
fun assertIsTrue(condition: Boolean) {
  if (!condition) {
    throw AssertionError("Expected condition to be true");
  }
}

fun visibility(show: Boolean) = if (show) {
  View.VISIBLE
} else {
  View.GONE
}

fun visibilityOpacity(show: Boolean) = if (show) {
  1.0f
} else {
  0.0f
}
