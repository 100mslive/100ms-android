package live.hms.app2.ui.meeting.activespeaker

import android.util.Log
import java.util.*

/**
 * Active Speaker LRU - Aims to fetch the most active speakers
 * in the order which requires minimum swaps from the previous
 * state -- ensuring that the UI is not updated too frequently.
 *
 * @param capacity - Maximum number of items in the [queue]
 */
class ActiveSpeakerLRU<T>(
  val capacity: Int
) {
  companion object {
    private const val TAG = "ActiveSpeakerLRU"
  }

  /**
   * Timer used to assign [Item.timestamp] values to each [Item]
   */
  private var timer = 0L

  /**
   * Wrapper over [T], adding [timestamp] to allow comparison
   * between two [Item]'s
   */
  private data class Item<T>(
    val value: T,
    var timestamp: Long
  )

  /**
   * Thread-safe queue which stores each [Item] each representing the
   * most active [T].
   *
   * When a new items needs to be pushed we follow algorithm defined in
   * [update] method.
   */
  private val queue = Collections.synchronizedList(ArrayList<Item<T>>())

  /**
   * Gets the current number of elements in the queue
   */
  val size: Int
    @Synchronized get() = queue.size

  private fun getItemIndex(value: T): Int {
    for ((i, item) in queue.withIndex()) {
      if (value == item.value) {
        return i
      }

    }

    return -1
  }

  private fun getMinTimestampIndex(): Int {
    if (queue.isEmpty()) return -1

    var idx = 0
    var minTimeItem = queue.first()

    for ((i, item) in queue.withIndex()) {
      if (item.timestamp < minTimeItem.timestamp) {
        minTimeItem = item
        idx = i
      }
    }

    return idx
  }

  /**
   * Resets the [Item.timestamp] values starting with 0
   * This prevents the overflow of [timer] values
   */
  private fun normalize() {
    Log.d(TAG, "normalize: START $queue")
    timer = 0
    queue.sortedBy { it.timestamp }.forEach {
      it.timestamp = timer++
    }
    Log.d(TAG, "normalize: DONE $queue")
  }

  /**
   *
   * For each current item in [items]:
   *  1. Check if current value exists already in the [queue] - update
   *  the timestamp to [timer] + 1
   *  2. Else if [size] < [capacity] - push the [Item] simply
   *  into the [queue]
   *  3. Else, find and replace the item with minimum [Item.timestamp]
   *  in the queue with the current item
   *
   * @param items - List of items to be pushed / updated in the [queue]
   *  The item are assumed to be sorted from loud -> silent
   */
  @Synchronized
  fun update(items: List<T>) {
    var index = 0
    for (item in items.take(capacity).asReversed()) {
      val position = getItemIndex(item)
      if (position == -1) {
        // New entry
        if (queue.size == capacity) {
          // Replace the oldest item with this one
          val idx = getMinTimestampIndex()
          queue[idx] = Item(item, timer++)
        } else {
          // Consider this as loudest speaker, hence add it as
          // first item
          queue.add(0, Item(item, timer++))
        }
      } else {
        // Item already in the queue, simply update the timer
        queue[position].timestamp = timer++
      }

      index += 1
    }

    if (timer > 10000) {
      normalize()
    }
  }

  /**
   * Removes any item from the [queue]
   */
  @Synchronized
  fun remove(items: List<T>) {
    for (item in items) {
      val position = getItemIndex(item)
      if (position != -1) {
        queue.removeAt(position)
      }
    }
  }

  /**
   * Simply get all the elements in the [queue] maintaining
   * the order
   */
  @Synchronized
  fun getItemsInOrder() = queue.map { it.value }
}