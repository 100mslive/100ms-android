package live.hms.app2.helpers

import android.view.View

/**
 * Ignores the onClick if the user clicks on the View
 * within the provided delay.
 * This extension is helpful when the user can spam-click
 * the view multiple time eventually leading to undefined
 * results.
 */
class OnSingleClickListener : View.OnClickListener {

  companion object {
    const val DEFAULT_DELAY_MILLIS = 100L
  }

  private val onClickListener: View.OnClickListener
  private val delay: Long

  private var previousClickTimeMillis = 0L

  constructor(listener: View.OnClickListener, delay: Long = DEFAULT_DELAY_MILLIS) {
    onClickListener = listener
    this.delay = delay
  }

  constructor(listener: (View) -> Unit, delay: Long = DEFAULT_DELAY_MILLIS) {
    onClickListener = View.OnClickListener { listener.invoke(it) }
    this.delay = delay
  }

  override fun onClick(v: View) {
    val currentTimeMillis = System.currentTimeMillis()

    if (currentTimeMillis >= previousClickTimeMillis + delay) {
      previousClickTimeMillis = currentTimeMillis
      onClickListener.onClick(v)
    }
  }
}
