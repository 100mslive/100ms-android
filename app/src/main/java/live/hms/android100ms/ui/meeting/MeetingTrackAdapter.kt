package live.hms.android100ms.ui.meeting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils
import live.hms.android100ms.databinding.ListItemMeetingTrackBinding
import org.webrtc.RendererCommon

class MeetingTrackAdapter(
    private val tracks: ArrayList<MeetingTrack>
) : RecyclerView.Adapter<MeetingTrackAdapter.MeetingTrackViewHolder>() {

    inner class MeetingTrackViewHolder(val binding: ListItemMeetingTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(track: MeetingTrack) {
            if (HMSWebRTCEglUtils.getRootEglBaseContext() == null) HMSWebRTCEglUtils.getRootEglBase()

            binding.surfaceView.apply {
                init(HMSWebRTCEglUtils.getRootEglBaseContext(), null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                track.videoTrack?.addSink(this)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingTrackViewHolder {
        val binding = ListItemMeetingTrackBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MeetingTrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MeetingTrackViewHolder, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount() = tracks.size
}