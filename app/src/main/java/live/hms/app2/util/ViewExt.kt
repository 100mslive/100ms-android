package live.hms.app2.util

import android.view.View
import live.hms.app2.helpers.OnSingleClickListener

fun View.setOnSingleClickListener(l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l))
}

fun View.setOnSingleClickListener(l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l))
}

// Keep the listener at last such that we can use kotlin lambda
fun View.setOnSingleClickListener(waitDelay: Long, l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l, waitDelay))
}

fun View.setOnSingleClickListener(waitDelay: Long, l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l, waitDelay))
}
