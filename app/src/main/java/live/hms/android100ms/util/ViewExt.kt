package live.hms.android100ms.util

import android.view.View
import live.hms.android100ms.helpers.OnSingleClickListener

fun View.setOnSingleClickListener(l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l))
}

fun View.setOnSingleClickListener(l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l))
}

// Keep the listener at last such that we can use kotlin lambda
fun View.setOnSingleClickListener(delay: Long, l: View.OnClickListener) {
  setOnClickListener(OnSingleClickListener(l, delay))
}

fun View.setOnSingleClickListener(delay: Long, l: (View) -> Unit) {
  setOnClickListener(OnSingleClickListener(l, delay))
}
