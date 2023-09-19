package live.hms.roomkit.ui.meeting.participants

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentParticipantsBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
class ParticipantsFragment : Fragment() {

    private val TAG = "ParticipantsFragment"
    private var binding by viewLifecycle<FragmentParticipantsBinding>()
    private var alertDialog: AlertDialog? = null
    val adapter = GroupieAdapter()
    private lateinit var handRaisedKey :String
    private var filterText : String? = null
    private fun isSearching() = !filterText.isNullOrEmpty()
    private val expandedGroups = mutableMapOf<String,Boolean>()

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
        handRaisedKey = requireContext().resources.getString(R.string.hand_raised_group)
        initViewModels()
        return binding.root
    }

    // This is only suspending so it can run in the background
    private suspend fun updateParticipantsAdapter(peerList: List<HMSPeer>) {
        // Don't throw away results when it's searching
        //  ideally this should be replaced with just updating the
        //  peers but still with the search query.

        // Group people by roles.
        val groupedPeers : Map<String, List<HMSPeer>> = peerList.groupBy {
            if(CustomPeerMetadata.fromJson(it.metadata)?.isHandRaised == true)
                handRaisedKey
            else
                it.hmsRole.name
        }


        val canChangeRole = meetingViewModel.isAllowedToChangeRole()
        val canMutePeers = meetingViewModel.isAllowedToMutePeers()
        val canRemovePeers = meetingViewModel.isAllowedToRemovePeers()
        val localPeer = meetingViewModel.hmsSDK.getLocalPeer()!!

        val groups = mutableListOf<ExpandableGroup>()
        // Keep hand raised on top.
        if(groupedPeers[handRaisedKey] != null) {
            groups.add(keyToGroup(handRaisedKey, groupedPeers, canChangeRole, canMutePeers, canRemovePeers, localPeer))
        }

        groups.addAll(groupedPeers.keys.filterNot { it == handRaisedKey }.map { key ->
            keyToGroup(key, groupedPeers, canChangeRole, canMutePeers, canRemovePeers, localPeer)
        })

        adapter.update(groups)
    }


    private fun expandedGroups( rolename : String, expanded : Boolean) {
        expandedGroups[rolename] = expanded
    }

    private fun keyToGroup(
        key: String,
        groupedPeers: Map<String, List<HMSPeer>>,
        canChangeRole: Boolean,
        canMutePeers: Boolean,
        canRemovePeers: Boolean,
        localPeer : HMSLocalPeer
    ) : ExpandableGroup =
        ExpandableGroup(ParticipantHeaderItem(key, groupedPeers[key]?.size, ::expandedGroups))
            .apply {
                addAll(groupedPeers[key]?.map {
                    ParticipantItem(it,
                        localPeer,
                        meetingViewModel::togglePeerMute,
                        ::changePeerRole,
                        canChangeRole,
                        canMutePeers,
                        canRemovePeers,
                        meetingViewModel.prebuiltInfoContainer,
                        meetingViewModel.participantPreviousRoleChangeUseCase,
                        meetingViewModel::requestPeerLeave,
                        meetingViewModel.activeSpeakers
                    )
                }!!)
                // If the group was expanded, open it again.
                if(expandedGroups[key] == true || expandedGroups[key] == null){
                    onToggleExpanded()
                }
            }

    private fun changePeerRole(remotePeerId : String, toRole : String, force : Boolean) =
        meetingViewModel.changeRole(remotePeerId, toRole, force)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        initOnBackPress()
        initViews()
    }
    private fun updateParticipantCount(count : Int) {
        binding.participantsNum.text = resources.getString(R.string.participants_heading, count)
    }

    private fun initViews() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            addItemDecoration(
                // Border bright
                HeaderItemDecoration(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.borderBright,
                        HMSPrebuiltTheme.getDefaults().border_bright
                    ),
                    0,
                    16f,
                    24f,
                    R.layout.participant_header_item
                )
            )
            val divider = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL).apply {
                setDrawable(resources.getDrawable(R.drawable.participants_divider)!!)
            }
            addItemDecoration(divider)
        }
        binding.closeButton.setOnSingleClickListener {
            closeButton()
        }
        // Search disables conventional updates.
        binding.textInputSearch.apply {
            addTextChangedListener { text ->
                lifecycleScope.launch {
                    filterText = text.toString()
                    updateParticipantsAdapter(getSearchFilteredPeersIfNeeded())
                }
            }
        }
    }

    private fun closeButton() {
        parentFragmentManager
            .beginTransaction()
            .remove(this)
            .commitAllowingStateLoss()
    }

    private fun getSearchFilteredPeersIfNeeded() : List<HMSPeer> {
        val text = filterText

        return if (!isSearching())
            meetingViewModel.peers
        else
            meetingViewModel.peers.filter {
                text.isNullOrEmpty() || it.name.contains(
                    text.toString(),
                    true
                )
            }
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        binding.recyclerView.adapter = adapter
        // Initial updating of views
        meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                updateParticipantsAdapter(getSearchFilteredPeersIfNeeded())
            }
        }
        meetingViewModel.peerCount.observe(viewLifecycleOwner,::updateParticipantCount)

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
                    closeButton()
                }
            })
    }

}