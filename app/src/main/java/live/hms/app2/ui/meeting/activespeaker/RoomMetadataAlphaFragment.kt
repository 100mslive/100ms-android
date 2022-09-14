package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.hms.app2.databinding.RoomMetadataAlphaLayoutBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle

class RoomMetadataAlphaFragment : Fragment() {
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private var binding by viewLifecycle<RoomMetadataAlphaLayoutBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RoomMetadataAlphaLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setMetadataButton.setOnClickListener {
            meetingViewModel.setSessionMetadata(binding.rtmpWidth.text.toString())
        }

        binding.getMetadataButton.setOnClickListener {
            meetingViewModel.getSessionMetadata()
        }

        meetingViewModel.sessionMetadata.observe(viewLifecycleOwner) {
            binding.currentSessionMetadata.text = "$it"
        }
    }

    override fun onResume() {
        super.onResume()
        meetingViewModel.getSessionMetadata()
    }
}