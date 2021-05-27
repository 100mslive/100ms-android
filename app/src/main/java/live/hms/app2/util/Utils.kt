package live.hms.app2.util;

import android.view.View


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