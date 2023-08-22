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
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
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

    // This is only suspending so it can run in the background
    private suspend fun updateParticipantsAdapter() {
        val handRaisedKey = "Hand Raised"
        // Group people by roles.
        val groupedPeers : Map<String, List<HMSPeer>> = meetingViewModel.peers.groupBy {
            if(CustomPeerMetadata.fromJson(it.metadata)?.isHandRaised == true && it.hmsRole.name.lowercase() != "broadcaster" && it.hmsRole.name.lowercase() != "host")
                handRaisedKey
            else
                it.hmsRole.name
        }


        val canChangeRole = meetingViewModel.isAllowedToChangeRole()
        val canMutePeers = meetingViewModel.isAllowedToMutePeers()
        val canRemovePeers = meetingViewModel.isAllowedToRemovePeers()

        val groups = mutableListOf<ExpandableGroup>()
        // Keep hand raised on top.
        if(groupedPeers[handRaisedKey] != null) {
            groups.add(keyToGroup(handRaisedKey, groupedPeers, canChangeRole, canMutePeers, canRemovePeers))
        }

        groups.addAll(groupedPeers.keys.filterNot { it == handRaisedKey }.map { key ->
            keyToGroup(key, groupedPeers, canChangeRole, canMutePeers, canRemovePeers)
        })

        adapter.update(groups)
    }

    private fun keyToGroup(
        key: String,
        groupedPeers: Map<String, List<HMSPeer>>,
        canChangeRole: Boolean,
        canMutePeers: Boolean,
        canRemovePeers: Boolean
    ) : ExpandableGroup =
        ExpandableGroup(ParticipantHeaderItem(key, groupedPeers[key]?.size))
            .apply {
                addAll(groupedPeers[key]?.map {
                    ParticipantItem(it,
                        meetingViewModel::togglePeerMute,
                        ::togglePeerMedia,
                        canChangeRole,
                        canMutePeers,
                        canRemovePeers
                    )
                }!!)
            }

    private fun togglePeerMedia(remotePeerId : String) {
            val toRole = meetingViewModel.getAvailableRoles()
                .find { role-> role.name.contains("viewer") || role.name.contains("guest") }
            if (toRole != null) {
                meetingViewModel.changeRole(remotePeerId, toRole.name, true)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnBackPress()
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
        binding.recyclerView.adapter = adapter
        // Initial updating of views
        meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
            val peers = meetingViewModel.peers
            binding.participantCount.text = "${peers.count()}"
            lifecycleScope.launch {
                updateParticipantsAdapter()
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