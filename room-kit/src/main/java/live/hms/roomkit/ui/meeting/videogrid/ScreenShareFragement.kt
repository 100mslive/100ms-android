package live.hms.roomkit.ui.meeting.videogrid

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetScreenShareBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import org.webrtc.RendererCommon

class ScreenShareFragement(val screenShareTrackId: String) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ScreenShareFragement"
    }

    private var binding by viewLifecycle<BottomSheetScreenShareBinding>()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetScreenShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.root.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDefault,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )
        binding.localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        meetingViewModel.tracks.observe(viewLifecycleOwner) { it ->

            val track = it.find { it.video?.trackId == screenShareTrackId }?.video
            if (track != null) {
                binding.localVideoView.addTrack(track)
            }
        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        contextSafe { context, activity ->
            binding.localVideoView.removeTrack()
        }
    }


}