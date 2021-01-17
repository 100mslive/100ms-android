package live.hms.android100ms.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.android100ms.R
import live.hms.android100ms.databinding.ModalMeetingBottomSheetBinding
import live.hms.android100ms.util.viewLifecycle

class MeetingOptionsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "MeetingOptionsBottomSheet"
    }

    private var binding by viewLifecycle<ModalMeetingBottomSheetBinding>()
    private val args: MeetingOptionsBottomSheetArgs by navArgs()

    private val meetingViewModel: MeetingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ModalMeetingBottomSheetBinding.inflate(inflater, container, false)
        initButtons(args.metadata)
        return binding.root
    }

    private fun initButtons(metadata: MeetingOptionsMetadata) {
        binding.buttonToggleAudio.apply {
            setIconResource(
                if (metadata.isAudioEnabled)
                    R.drawable.ic_baseline_music_note_24
                else
                    R.drawable.ic_baseline_music_off_24
            )
        }

        binding.buttonToggleVideo.apply {
            setIconResource(
                if (metadata.isVideoEnabled)
                    R.drawable.ic_baseline_videocam_24
                else
                    R.drawable.ic_baseline_videocam_off_24
            )
        }

        mapOf(
            binding.buttonToggleVideo to MeetingOptions.TOGGLE_VIDEO,
            binding.buttonToggleAudio to MeetingOptions.TOGGLE_AUDIO,
            binding.buttonChat to MeetingOptions.OPEN_CHAT,
            binding.buttonEndCall to MeetingOptions.END_CALL,
            binding.buttonFlipCamera to MeetingOptions.FLIP_CAMERA,
            binding.buttonSettings to MeetingOptions.OPEN_SETTINGS,
            binding.buttonShare to MeetingOptions.SHARE,
        ).forEach {
            val option = it.value
            it.key.setOnClickListener {
                // Hacky-fix, use dismiss()
                findNavController().navigateUp()

                // Fire the event
                meetingViewModel.selectOption(option)
            }
        }
    }
}