package live.hms.roomkit.ui.meeting.participants

import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer

const val handRaisedKey = "Hand Raised"
class ParticipantsUseCase(val meetingViewModel: MeetingViewModel,
                          val getPeers : () -> List<HMSPeer>,
                            val onClick: (role: String) -> Unit) {
    val adapter = GroupieAdapter()
    private val expandedGroups = mutableMapOf<String,Boolean>()
    private var filterText : String? = null
    private fun isSearching() = !filterText.isNullOrEmpty()

    // Used to use meetingViewModel.peers
    private fun getSearchFilteredPeersIfNeeded(peers : List<HMSPeer>) : List<HMSPeer> {
        val text = filterText

        return if (!isSearching())
            peers
        else
            peers.filter {
                text.isNullOrEmpty() || it.name.contains(
                    text.toString(),
                    true
                )
            }
    }

    fun initSearchView(textInputSearch : TextInputEditText, scope : LifecycleCoroutineScope) {
        textInputSearch.apply {
            addTextChangedListener { text ->
                scope.launch {
                    filterText = text.toString()
                    updateParticipantsAdapter(getPeers())
                }
            }
        }
    }
    fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(recyclerView.context)
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
            val divider = DividerItemDecoration(recyclerView.context, RecyclerView.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(recyclerView.context.resources, R.drawable.participants_divider, null)!!)
            }
            addItemDecoration(divider)
        }
    }
    // This is only suspending so it can run in the background
    suspend fun updateParticipantsAdapter(peers: List<HMSPeer>, hasNext: Boolean = true) {
        // Don't throw away results when it's searching
        //  ideally this should be replaced with just updating the
        //  peers but still with the search query.
        val peerList = getSearchFilteredPeersIfNeeded(peers)
        val localPeerRoleName = meetingViewModel.hmsSDK.getLocalPeer()!!.hmsRole.name

        // Group people by roles.
        val groupedPeers : Map<String, List<HMSPeer>> = peerList.groupBy {
            if(it.isHandRaised)
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
                .also {
                    // Add view more here
                    // Offstage roles
                    if(meetingViewModel.isLargeRoom() && meetingViewModel.prebuiltInfoContainer.offStageRoles(localPeerRoleName)?.contains(key) == true) {
                        if (hasNext) {
                            it.add(ViewMoreItem(key) { role -> onClick(role) })
                        }
                    }
                }
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
                        meetingViewModel::changeRole,
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

}