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
import live.hms.video.sdk.models.PeerListIterator


const val handRaisedKey = "Hand Raised"
class ParticipantsUseCase(val meetingViewModel: MeetingViewModel,
                          val getPeers : () -> List<HMSPeer>,
                          val getNextPage: (role: String) -> Unit,
                          val lifecycleCoroutineScope : LifecycleCoroutineScope
    ) {
    // When
    val adapter = GroupieAdapter()
    private val expandedGroups = mutableMapOf<String,Boolean>()
    private var filterText : String? = null
    private var filterGroup : String? = null
    suspend fun roleFiltering(filterByGroupType : String?)
    {
//        when(filterByGroupType) {
//            is GroupType.HandRaised -> handRaisedKey
//            is GroupType.Role -> filterByGroupType.hmsRole.name
//            null -> null
//        }
        filterGroup = filterByGroupType
        // Trigger a refresh.
        updateParticipantsAdapter(getPeers())
    }
    private fun isSearching() = !filterText.isNullOrEmpty()

    private fun getGroupFilteredPeersIfNeeded(peers: List<HMSPeer>) : List<HMSPeer> {
        val filterGroup = filterGroup
        return if(filterGroup.isNullOrEmpty()) {
            peers
        } else {
            peers.filter {
                val filterGroupName = if(it.isHandRaised)
                handRaisedKey
            else
                it.hmsRole.name
                filterGroupName == filterGroup
            }
        }
    }
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
    suspend fun updateParticipantsAdapter(peers: List<HMSPeer>, iteratorMap: Map<String, PeerListIterator>? = null) {
        // Don't throw away results when it's searching
        //  ideally this should be replaced with just updating the
        //  peers but still with the search query.
        val peerList = getSearchFilteredPeersIfNeeded(getGroupFilteredPeersIfNeeded(peers))
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
            // Send peercount as null as we want to show the count from real time list
            groups.add(keyToGroup(handRaisedKey, groupedPeers, canChangeRole, canMutePeers, canRemovePeers, localPeer, null))
        }

        groups.addAll(groupedPeers.keys.filterNot { it == handRaisedKey }.map { key ->
            val isNonRealTimeHeader = meetingViewModel.isLargeRoom() && meetingViewModel.prebuiltInfoContainer.offStageRoles(localPeerRoleName)?.contains(key) == true
            // For large rooms and off-stage roles, show peer count from the response of iterator
            val peerCount = if (isNonRealTimeHeader) {
                iteratorMap?.get(key)?.totalCount
            } else {
                groupedPeers[key]?.size
            }
            keyToGroup(key, groupedPeers, canChangeRole, canMutePeers, canRemovePeers, localPeer, peerCount)
                .also {
                    // Add view more here for non realtime roles
                    // Show the button to all the peers of this role
                    if(isNonRealTimeHeader && filterGroup.isNullOrEmpty()) {
                        val hasNext = iteratorMap?.get(key)?.hasNext() ?: false
                        if (hasNext) {
                            it.add(ViewMoreItem(key, "View All") { role : String ->
                                lifecycleCoroutineScope.launch { roleFiltering(role) }
//                                onClick(role)
                            })
                        }
                    } else if(isNonRealTimeHeader){
                        // Show the button to load more of the peers in this group
                        val hasNext = iteratorMap?.get(key)?.hasNext() ?: false
                        if (hasNext) {
                            it.add(ViewMoreItem(key, "View More") { role : String ->
                                getNextPage(role)
                            })
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
        localPeer : HMSLocalPeer,
        totalPeerCount: Int?
    ) : ExpandableGroup {
        // Show the number of peers in the current role if this is a large room
        // Else show the count as the size of the peer list for regular rooms
        val numPeers = totalPeerCount ?: groupedPeers[key]?.size
        return ExpandableGroup(
                ParticipantHeaderItem(key, numPeers, ::expandedGroups),
                isExpanded(key)
            )
                .apply {
                    addAll(groupedPeers[key]?.map {
                        ParticipantItem(
                            it,
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
                }
    }

    private fun isExpanded(key: String): Boolean =
        expandedGroups[key] == true || expandedGroups[key] == null

}