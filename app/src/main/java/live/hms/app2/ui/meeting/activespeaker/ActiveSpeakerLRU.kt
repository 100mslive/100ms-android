package live.hms.app2.ui.meeting.activespeaker

import java.util.*
import kotlin.math.ceil

class ActiveSpeakerLRU<T>(val capacity: Int) {
  private val maxPushAtATime = ceil(capacity / 3.0).toInt()

  private var entryIndex = 0L

  private data class Item<T>(
    val value: T,
    var timestamp: Long
  )

  private val queue = LinkedList<Item<T>>()

  private fun getItemWithMinimumTime(): T {
    var res = queue.first
    for (item in queue) {
      if (res.timestamp > item.timestamp) {
        res = item
      }
    }

    return res.value
  }

  private fun getItem(value: T): Item<T>? {
    for (item in queue) {
      if (value == item.value) {
        return item
      }
    }

    return null
  }

  /**
   * Iterate over the first [maxPushAtATime] items.
   * For each item,
   *  - Get the [Item] with lowest [Item.timestamp]
   *  - Replace this item with the current one
   *  - If current item is first, then swap with 1st element in queue
   */
  fun push(items: Array<T>) {
    var index = 0
    for (item in items) {
      var inQueueItem = getItem(item)
      if (inQueueItem == null) {
        if (queue.size == capacity) {
          queue.removeLast()
        }

        inQueueItem = Item(item, entryIndex++)
        queue.addFirst(inQueueItem)

      } else if (index == 0) {
        queue.remove(inQueueItem)
        inQueueItem.timestamp = entryIndex++
        queue.addFirst(inQueueItem)
      }

      index += 1
      if (index == maxPushAtATime) break
    }
  }

  fun getItemsInOrder() = queue.map { it.value }
}