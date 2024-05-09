package live.hms.roomkit.ui.meeting.participants

import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.error.HMSException
import live.hms.video.sdk.listeners.PeerListResultListener
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.PeerListIterator


const val handRaisedKey = "Hand Raised"
class ParticipantsUseCase(val meetingViewModel: MeetingViewModel,
                          private val scope : LifecycleCoroutineScope,
                          viewLifecycleOwner : LifecycleOwner,
                          val enterGroupFiltering : () -> Unit
    ) {
    // When
    val adapter = GroupieAdapter()
    private val expandedGroups = mutableMapOf<String,Boolean>()
    private var filterText : String? = null
    private var filterGroup : String? = null

    private val offStageRoleNames = { meetingViewModel.prebuiltInfoContainer.offStageRoles(meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole?.name!!)}
    private val isLargeRoom = meetingViewModel.hmsSDK.getRoom()?.isLargeRoom == true
    private val getPeerListIterator = meetingViewModel::getPeerlistIterator
    private val paginatedPeers : PaginatedPeers = PaginatedPeers(offStageRoleNames, getPeerListIterator, isLargeRoom, scope)
    private val getAllPeers : () -> List<HMSPeer> = { meetingViewModel.peers + if (isLargeRoom) {
        paginatedPeers.peers
    } else {
        emptyList()
    }}
    init {
        // Initial page load.
        scope.launch {
            paginatedPeers.refreshNonRealtimePeersIfNeeded()
            updateParticipantsAdapter(getAllPeers())
        }
    }

    val participantPeerUpdate = meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
        scope.launch {
            periodicallyUpdatePeerListForLargeRoom()
            updateParticipantsAdapter(getAllPeers())
        }
    }

    suspend fun roleFiltering(filterByGroupType : String?)
    {
//        when(filterByGroupType) {
//            is GroupType.HandRaised -> handRaisedKey
//            is GroupType.Role -> filterByGroupType.hmsRole.name
//            null -> null
//        }
        if(filterByGroupType != null)
            enterGroupFiltering()
        filterGroup = filterByGroupType
        // Trigger a refresh.
        updateParticipantsAdapter(getAllPeers())
    }
    private fun isSearching() = !filterText.isNullOrEmpty()
    private fun isInViewMore() = !filterGroup.isNullOrEmpty()

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

    fun initSearchView(textInputSearch : EditText, scope : LifecycleCoroutineScope) {
        textInputSearch.apply {
            addTextChangedListener { text ->
                scope.launch {
                    filterText = text.toString()
                    updateParticipantsAdapter(getAllPeers())
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
    suspend fun updateParticipantsAdapter(allPeers: List<HMSPeer>) {
        // Don't throw away results when it's searching
        //  ideally this should be replaced with just updating the
        //  peers but still with the search query.
        val peerList = getSearchFilteredPeersIfNeeded(getGroupFilteredPeersIfNeeded(allPeers))
        // This can be null during fragment recreation, where the peer hasn't really joined yet.
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
                paginatedPeers.iteratorMap[key]?.totalCount
            } else {
                groupedPeers[key]?.size
            }
            keyToGroup(key, groupedPeers, canChangeRole, canMutePeers, canRemovePeers, localPeer, peerCount)
                .also {
                    // Add view more here for non realtime roles
                    // Show the button to all the peers of this role
                    if(isNonRealTimeHeader && filterGroup.isNullOrEmpty()) {
                        val hasNext = paginatedPeers.iteratorMap[key]?.hasNext() ?: false
                        if (hasNext) {
                            it.add(ViewMoreItem(key, "View All") { role : String ->
                                scope.launch { roleFiltering(role) }
                            })
                        }
                    } else if(isNonRealTimeHeader){
                        // Show the button to load more of the peers in this group
                        val hasNext = paginatedPeers.iteratorMap.get(key)?.hasNext() ?: false
                        if (hasNext) {
                            it.add(ViewMoreItem(key, "View More") { role : String ->
                                scope.launch {
                                    // This updates the peers in paginatedPeers.peers
                                    // Errors are ignored.
                                    paginatedPeers.getNextPage(role)
                                    // Reset the timer
                                    periodicallyUpdatePeerListForLargeRoom()
                                    // Update with all the peers.
                                    updateParticipantsAdapter(getAllPeers())
                                }
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
                            meetingViewModel.activeSpeakers,
                            meetingViewModel::lowerRemotePeerHand
                        )
                    }!!)
                }
    }

    private fun isExpanded(key: String): Boolean =
        expandedGroups[key] == true || expandedGroups[key] == null

    fun clear() {
        // This throws away all peers loaded with "load more"
        paginatedPeers.clear()
    }

    private var updatePeerListJob  : Job? = null
    private fun periodicallyUpdatePeerListForLargeRoom() {
        if(!isLargeRoom) return
        updatePeerListJob?.cancel()
        updatePeerListJob = scope.launch {
            while(true) {
                delay(5000) // But we don't want this to overlap with the other ways this happens
                // If it's in the top level list, keep updating
                val currentFilterGroup = filterGroup
                if(currentFilterGroup.isNullOrEmpty())
                    paginatedPeers.refreshNonRealtimePeersIfNeeded() // This has to stop when we're loading other peers
                else
                    paginatedPeers.refreshIterator(currentFilterGroup)
                updateParticipantsAdapter(getAllPeers())
            }
        }
    }

}

class PaginatedPeers(
    val offstageRoleNames: () -> List<String?>?,
    val getPeerListIterator: (roleName: String) -> PeerListIterator,
    val isLargeRoom : Boolean,
    val scope: LifecycleCoroutineScope
) {
    var peers : List<HMSPeer> = emptyList()

    suspend fun refreshNonRealtimePeersIfNeeded() {
        if (isLargeRoom) {
            peers = initPaginatedPeerListAndIterators()
        }
    }

    val iteratorMap = hashMapOf<String, PeerListIterator>()

    fun refreshIterator(role :String) {
        iteratorMap[role]?.hasNext()
    }
    private suspend fun getAndUpdateIteratorForRole(role: String): CompletableDeferred<ArrayList<HMSPeer>> {
        val iterator = getPeerListIterator(role)
        iteratorMap[role] = iterator
        val roleIteratorDeferred = CompletableDeferred<ArrayList<HMSPeer>>()
        // Get the first page
        iterator.next(object : PeerListResultListener {
            override fun onError(error: HMSException) {
                roleIteratorDeferred.completeExceptionally(error)
            }

            override fun onSuccess(result: ArrayList<HMSPeer>) {
                roleIteratorDeferred.complete(result)
            }
        })
        return roleIteratorDeferred
    }

    private suspend fun initPaginatedPeerListAndIterators(): List<HMSPeer> {
        // Now fetch the first set of peers for all off-stage roles
        val offStageRoleNames = offstageRoleNames()
        val deferredList = offStageRoleNames?.filterNotNull()?.map { role ->
            getAndUpdateIteratorForRole(role)
        }
        // This can throw an exception
        return supervisorScope { deferredList?.awaitAll()?.flatten() ?: emptyList() }
    }
    suspend fun getNextPage(role: String) {
        val iterator = iteratorMap[role]
        val peerListDeferred = CompletableDeferred<List<HMSPeer>>()
        iterator?.next(object : PeerListResultListener {
            override fun onError(error: HMSException) {
                peerListDeferred.completeExceptionally(error)
//                meetingViewModel.triggerErrorNotification(message = error.message)
            }

            override fun onSuccess(result: ArrayList<HMSPeer>) {
                // Tehcnically this should never be called on this fragment.
                // add the next page of peers into final list
                peerListDeferred.complete(result)
//                paginatedPeerList.addAll(result)
//                lifecycleScope.launch {
//                    periodicallyUpdatePeerListForLargeRoom() // Warning this resets the peerlit.
//                    participantsUseCase.updateParticipantsAdapter(getPeerList(), iteratorMap)
//                }
            }

        })

        try{
            peers = peers.plus(peerListDeferred.await())

        } catch (ex : HMSException) {
            // handle error
        }
    }

    fun clear() {
        iteratorMap.clear()
        peers = emptyList()
    }
}