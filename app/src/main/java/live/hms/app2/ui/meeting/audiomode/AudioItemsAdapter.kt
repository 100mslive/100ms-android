package live.hms.app2.ui.meeting.audiomode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemAudioBinding
import live.hms.app2.util.NameUtils
import live.hms.app2.util.visibility

class AudioItemsAdapter : RecyclerView.Adapter<AudioItemsAdapter.AudioViewHolder>() {

  private val items = ArrayList<AudioItem>()

  @MainThread
  @Synchronized
  fun setItems(newItems: List<AudioItem>) {
    val callback = AudioItemsDiffUtil(
      items.map { it.clone() }.toTypedArray(),
      newItems.map { it.clone() }.toTypedArray()
    )
    val diff = DiffUtil.calculateDiff(callback)

    items.clear()
    items.addAll(newItems)
    diff.dispatchUpdatesTo(this)
  }

  inner class AudioViewHolder(
    private val binding: ListItemAudioBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: AudioItem) = binding.apply {
      name.text = item.name
      nameInitials.text = NameUtils.getInitials(item.name)

      setTrackStatus(item.isTrackMute)
      setAudioLevel(item.audioLevel)
    }

    fun setTrackStatus(isMute: Boolean) {
      binding.iconAudioOff.visibility = visibility(isMute)
    }

    fun setAudioLevel(level: Int) {
      binding.apply {
        when {
          level >= 20 -> {
            container.strokeWidth = 4
          }
          else -> {
            container.strokeWidth = 0
          }
        }
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
    val binding = ListItemAudioBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return AudioViewHolder(binding)
  }

  override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun onBindViewHolder(
    holder: AudioViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    if (payloads.isNotEmpty()) {
      when (payloads[0] as AudioItemsDiffUtil.PayloadKey) {
        AudioItemsDiffUtil.PayloadKey.AUDIO_LEVEL -> holder.setAudioLevel(items[position].audioLevel)
        AudioItemsDiffUtil.PayloadKey.TRACK_STATUS -> holder.setTrackStatus(items[position].isTrackMute)
        AudioItemsDiffUtil.PayloadKey.SAME -> Unit
      }

      return
    }

    super.onBindViewHolder(holder, position, payloads)
  }

  override fun getItemCount(): Int = items.size
}