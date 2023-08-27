package live.hms.roomkit.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.videogrid.VideoGridBaseFragment
import live.hms.roomkit.ui.meeting.pinnedvideo.StatsInterpreter
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.*
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSRemoteVideoTrack
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.utils.HMSLogger
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class ActiveSpeakerFragment : Fragment()  {

  companion object {
    private const val TAG = "ActiveSpeakerFragment"
  }

  private var binding by viewLifecycle<FragmentActiveSpeakerBinding>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)

    return binding.root
  }


}