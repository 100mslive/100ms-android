package live.hms.roomkit.ui.meeting.participants

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.databinding.FragmentParticipantsBinding
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.HMSPeer

class ParticipantsFragment : BottomSheetDialogFragment() {

    private val TAG = "ParticipantsFragment"
    private var binding by viewLifecycle<FragmentParticipantsBinding>()
    private var alertDialog: AlertDialog? = null
    val adapter = GroupieAdapter()

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
        binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        initViewModels()
        return binding.root
    }

    private suspend fun groupieAdapter() {
        binding.recyclerView.adapter = adapter
        // Group people by roles.
        val groupedPeers : Map<String, List<HMSPeer>> = meetingViewModel.peers.groupBy { it.hmsRole.name }

        val groups = groupedPeers.keys.map { key ->
            ExpandableGroup(ParticipantHeaderItem(key, groupedPeers[key]?.size))
                .apply {
                    addAll(groupedPeers[key]?.map { ParticipantItem(it) }!!)
                }
        }
        adapter.clear()
        adapter.addAll(groups)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnBackPress()
//        adapter =
//            ParticipantsAdapter(
//                meetingViewModel.isAllowedToChangeRole(),
//                meetingViewModel.isAllowedToRemovePeers(),
//                meetingViewModel.isAllowedToMutePeers(),
//                meetingViewModel.isAllowedToAskUnmutePeers(),
//                this::onSheetClicked
//            )
        initViews()
    }

    private fun initViews() {
        binding.participantCount.text = "0"
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
//            adapter = this@ParticipantsFragment.adapter
        }

        // Search is currently disabled
        binding.textInputSearch.apply {
            addTextChangedListener { text ->
                val items = meetingViewModel
                    .peers
                    .filter { text.isNullOrEmpty() || it.name.contains(text.toString(), true) }
//                adapter.setItems(items)
            }
        }
    }

    private fun onSheetClicked(peer: HMSPeer) {
        val action =
            ParticipantsFragmentDirections.actionParticipantsFragmentToBottomSheetRoleChange(
                peer.peerID,
                meetingViewModel.getAvailableRoles().map { it.name }.toTypedArray(),
                peer.name
            )
        findNavController().navigate(action)
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        meetingViewModel.peerLiveData.observe(viewLifecycleOwner) {
            val peers = meetingViewModel.peers
            binding.participantCount.text = "${peers.count()}"
            lifecycleScope.launch {
                groupieAdapter()
            }
        }

        meetingViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is MeetingState.NonFatalFailure) {

                alertDialog?.dismiss()
                alertDialog = null

                val message = state.exception.message

                val builder = AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setTitle(live.hms.roomkit.R.string.non_fatal_error_dialog_title)
                    .setCancelable(true)

                builder.setPositiveButton(live.hms.roomkit.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    alertDialog = null
                    meetingViewModel.setStatetoOngoing() // hack, so that the liveData represents the correct state. Use SingleLiveEvent instead
                }


                alertDialog = builder.create().apply { show() }
            }


        }
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
    }
}