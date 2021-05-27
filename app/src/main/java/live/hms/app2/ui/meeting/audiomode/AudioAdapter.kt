package live.hms.app2.ui.meeting.audiomode

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemAudioBinding
import live.hms.app2.util.NameUtils
import live.hms.video.sdk.models.HMSPeer

class AudioAdapter {
  private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

  inner class AudioViewHolder(
    private val binding: ListItemAudioBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: HMSPeer) {
      binding.name.text = item.name
      binding.nameInitials.text = NameUtils.getInitials(item.name)
      binding.iconAudioOff.visibility = v(item.audioTrack?.isMute != false)
    }
  }
}