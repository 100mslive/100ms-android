package live.hms.android100ms.ui.meeting

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brytecam.lib.webrtc.HMSWebRTCEglUtils
import live.hms.android100ms.databinding.ListItemMeetingTrackBinding
import org.webrtc.RendererCommon

class MeetingTrackAdapter(
    private val context: Context,
    private val tracks: ArrayList<MeetingTrack>,
    private val onItemClick: (track: MeetingTrack) -> Unit,
    private val onMeetingTrackPinned: (track: MeetingTrack) -> Unit,
) : RecyclerView.Adapter<MeetingTrackAdapter.MeetingTrackViewHolder>() {

    companion object {
        const val TAG = "MeetingTrackAdapter"
    }

    inner class MeetingTrackViewHolder(
        val binding: ListItemMeetingTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // TODO: Hold a reference to the track used earlier
        //  such that when ViewHolder is used again, we unbind
        //  the old track
        private var oldTrack: MeetingTrack? = null

        private fun tryReleasingSurfaceView(newTrack: MeetingTrack): Boolean {
            var isSurfaceViewInitialized = false
            oldTrack?.let { track ->
                Log.v(TAG, "Releasing SurfaceViewRenderer bind to $track")

                binding.surfaceView.apply {
                    track.videoTrack?.removeSink(this)
                    clearImage()

                    isSurfaceViewInitialized = true
                }

                // Remove the reference to the old-track
                null
            }

            // Update the reference
            oldTrack = newTrack

            return isSurfaceViewInitialized
        }

        fun bind(track: MeetingTrack) {
            Log.v(TAG, "binding track=${track}, (oldTrack=${oldTrack}")
            val isSurfaceViewInitialized = tryReleasingSurfaceView(track)

            binding.name.text = track.peer.userName
            binding.root.setOnClickListener { onItemClick(track) }
            binding.buttonPin.setOnClickListener { onMeetingTrackPinned(track) }

            binding.surfaceView.apply {
                if (!isSurfaceViewInitialized) {
                    init(HMSWebRTCEglUtils.getRootEglBaseContext(), null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                }
                track.videoTrack?.addSink(this)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingTrackViewHolder {
        val binding = ListItemMeetingTrackBinding.inflate(
            LayoutInflater.from(context),
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