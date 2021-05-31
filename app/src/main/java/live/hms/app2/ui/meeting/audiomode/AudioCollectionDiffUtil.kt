package live.hms.app2.ui.meeting.audiomode

import androidx.recyclerview.widget.DiffUtil

class AudioCollectionDiffUtil(
  private val oldCollections: ArrayList<AudioCollection>,
  private val newCollections: ArrayList<AudioCollection>,
) : DiffUtil.Callback() {

  enum class PayloadKey { ITEMS }

  override fun getOldListSize() = oldCollections.size
  override fun getNewListSize() = newCollections.size

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    return (oldItemPosition < oldCollections.size &&
        newItemPosition < newCollections.size &&
        oldCollections[oldItemPosition].title == newCollections[newItemPosition].title)
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    // Rely on the [AudioItemsDiffUtil] to take the diff between two items
    return false
  }

  override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    if (oldItemPosition >= oldCollections.size || newItemPosition >= newCollections.size) {
      return null
    }

    return PayloadKey.ITEMS
  }
}