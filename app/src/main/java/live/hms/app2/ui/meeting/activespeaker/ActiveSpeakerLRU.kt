package live.hms.app2.ui.meeting.activespeaker

class ActiveSpeakerLRU<T>(private val capacity: Int) {
  private var timer = 0L

  private data class Item<T>(
    val value: T,
    var timestamp: Long
  )

  private val queue = ArrayList<Item<T>>()

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

  fun update(items: List<T>) {
    var index = 0
    for (item in items) {
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
        queue[position].timestamp = timer++
      }

      index += 1
      if (queue.size == capacity) break
    }
  }

  fun remove(items: List<T>) {
    for (item in items) {
      val position = getItemIndex(item)
      if (position != -1) {
        queue.removeAt(position)
      }
    }
  }

  fun getItemsInOrder() = queue.map { it.value }
}