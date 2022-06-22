package live.hms.app2.ui.meeting.audiomode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.R
import live.hms.app2.databinding.ListItemAudioCollectionBinding
import live.hms.app2.util.visibility
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSSpeaker

class AudioCollectionAdapter : RecyclerView.Adapter<AudioCollectionAdapter.AudioViewHolder>() {

  private var collections = ArrayList<AudioCollection>()

  private fun dispatchUpdates(
    newCollections: ArrayList<AudioCollection>,
    isSpeakerUpdate: Boolean = false
  ) {
    val callback = AudioCollectionDiffUtil(collections, newCollections)
    val diff = DiffUtil.calculateDiff(callback)

    if (!isSpeakerUpdate) {
      collections.clear()
      collections.addAll(newCollections)
    }

    diff.dispatchUpdatesTo(this)
  }

  @MainThread
  @Synchronized
  fun setItems(newItems: List<HMSPeer>) {
    // NOTE: Make sure that the ordering of items is same
    //  Otherwise the UI is updated randomly
    val itemsMap = newItems.groupBy { it.hmsRole.name }
    val newCollections = ArrayList<AudioCollection>()

    for (item in itemsMap) {
      val audioItems = item.value
        .sortedBy { it.name }
        .map { AudioItem(it.peerID, it.name, it.audioTrack?.isMute ?: true) }
      newCollections.add(AudioCollection(item.key, ArrayList(audioItems)))
    }

    dispatchUpdates(newCollections)
  }

  @MainThread
  @Synchronized
  fun applySpeakerUpdates(speakers: Array<HMSSpeaker>) {
    var requiresUpdate = false

    for (collection in collections) {
      val toRemove = arrayListOf<AudioItem>()
      val toAdd = arrayListOf<AudioItem>()
      for (item in collection.items) {
        val level = speakers.find {
          it.peerId == item.peerId
        }?.level ?: 0

        if (item.audioLevel != level) {
          requiresUpdate = true
          toRemove.add(item)
          toAdd.add(AudioItem(item.peerId, item.name, item.isTrackMute, level))
        }
      }

      collection.items.removeAll(toRemove)
      collection.items.addAll(toAdd)
      collection.items.sortBy { it.name }
    }

    if (requiresUpdate) {
      dispatchUpdates(collections, true)
    }
  }

  inner class AudioViewHolder(
    private val binding: ListItemAudioCollectionBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(collection: AudioCollection) = binding.apply {
      title.text = collection.title
      audioCollection.apply {
        layoutManager = GridLayoutManager(context, 3)
        adapter = AudioItemsAdapter().apply { setItems(collection.items) }
      }

      buttonToggleVisibility.setOnClickListener {
        val show = audioCollection.visibility == View.GONE
        audioCollection.visibility = visibility(show)

        if (show) {
          buttonToggleVisibility.setIconResource(R.drawable.ic_keyboard_arrow_up_24)
        } else {

          buttonToggleVisibility.setIconResource(R.drawable.ic_keyboard_arrow_down_24)
        }
      }
    }

    fun updateWithCollection(collection: AudioCollection) {
      val adapter = binding.audioCollection.adapter as AudioItemsAdapter
      adapter.setItems(collection.items)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
    val binding = ListItemAudioCollectionBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return AudioViewHolder(binding)
  }

  override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
    holder.bind(collections[position])
  }

  override fun onBindViewHolder(
    holder: AudioViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    if (payloads.contains(AudioCollectionDiffUtil.PayloadKey.ITEMS)) {
      holder.updateWithCollection(collections[position])
      return
    }

    super.onBindViewHolder(holder, position, payloads)
  }

  override fun getItemCount(): Int = collections.size
}