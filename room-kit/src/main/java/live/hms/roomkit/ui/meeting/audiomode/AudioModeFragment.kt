package live.hms.roomkit.ui.meeting.audiomode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.roomkit.databinding.FragmentAudioBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.util.viewLifecycle

class AudioModeFragment : Fragment() {

  private val adapter = AudioCollectionAdapter()
  private var binding by viewLifecycle<FragmentAudioBinding>()

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentAudioBinding.inflate(inflater, container, false)
    initViews()
    initViewModels()
    return binding.root
  }

  private fun initViews() {
    binding.recyclerView.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = this@AudioModeFragment.adapter
    }
  }

  private fun initViewModels() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) {
      adapter.setItems(meetingViewModel.peers)
    }

    meetingViewModel.speakersLiveData.observe(viewLifecycleOwner) {
      synchronized(meetingViewModel.speakersLiveData) {
        adapter.applySpeakerUpdates(it)
      }

    }

    if (meetingViewModel.isLocalVideoEnabled.value != false) {
      meetingViewModel.toggleLocalVideo()
    }
  }
}