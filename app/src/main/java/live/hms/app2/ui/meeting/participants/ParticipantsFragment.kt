package live.hms.app2.ui.meeting.participants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.app2.databinding.FragmentParticipantsBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.meeting.MeetingViewModelFactory
import live.hms.app2.util.ROOM_DETAILS
import live.hms.app2.util.viewLifecycle

class ParticipantsFragment : Fragment() {

  private val adapter = ParticipantsAdapter()
  private var binding by viewLifecycle<FragmentParticipantsBinding>()

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentParticipantsBinding.inflate(inflater, container, false)
    initViews()
    initViewModels()
    return binding.root
  }

  private fun initViews() {
    binding.participantCount.text = "0"
    binding.recyclerView.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = this@ParticipantsFragment.adapter
    }

    binding.textInputSearch.apply {
      addTextChangedListener { text ->
        val items = meetingViewModel
          .peers
          .filter { text.isNullOrEmpty() || it.name.contains(text.toString(), true) }
          .toTypedArray()
        adapter.setItems(items)
      }
    }
  }

  private fun initViewModels() {
    meetingViewModel.tracks.observe(viewLifecycleOwner) {
      val peers = meetingViewModel.peers
      adapter.setItems(peers)
      binding.participantCount.text = "${peers.size}"
    }
  }
}