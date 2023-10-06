package live.hms.roomkit.ui.meeting.participants

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentParticipantsBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.error.HMSException
import live.hms.video.sdk.listeners.PeerListResultListener
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.PeerListIterator

class ParticipantsFragment : Fragment() {

    private val TAG = "ParticipantsFragment"
    private var binding by viewLifecycle<FragmentParticipantsBinding>()
    private var alertDialog: AlertDialog? = null
    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }
    private val participantsUseCase by lazy { ParticipantsUseCase(meetingViewModel, {
        if (isLargeRoom) {
            paginatedPeerList
        } else {
            meetingViewModel.peers
        }
    }, { role ->
        getNextPage(role)
    })
    }


    private val iteratorMap = hashMapOf<String, PeerListIterator>()
    private val paginatedPeerList = arrayListOf<HMSPeer>()
    private var isLargeRoom = false
    private var iteratorsInitated = false

    private fun getNextPage(role: String) {
        val iterator = iteratorMap[role]
        iterator?.next(object : PeerListResultListener {
            override fun onError(error: HMSException) {
                meetingViewModel.triggerErrorNotification(message = error.message)
            }

            override fun onSuccess(result: ArrayList<HMSPeer>) {
                // Tehcnically this should never be called on this fragment.
                // add the next page of peers into final list
                paginatedPeerList.addAll(result)
                lifecycleScope.launch {
                    periodicallyUpdatePeerListForLargeRoom() // Warning this resets the peerlit.
                    participantsUseCase.updateParticipantsAdapter(getPeerList(), iteratorMap)
                }
            }

        })
    }

    private suspend fun getPeerList(resetIterators : Boolean = false): List<HMSPeer> {
        return if (isLargeRoom) {
            if (!iteratorsInitated || resetIterators) {
                // Init before we begin
                iteratorsInitated = true
                try {
                    if(resetIterators) {
                        paginatedPeerList.clear()
                    }
                    paginatedPeerList.addAll(initPaginatedPeerListAndIterators())
                } catch (exception : HMSException) {
                    iteratorsInitated = false
                    Log.e("PaginatedPeerListError","$exception")
                }
            }
            // Return  the combined list of real time and non real time peers
            val realtimePeers = meetingViewModel.peers
            val filteredPaginatedPeers = paginatedPeerList.filter { !realtimePeers.contains(it) }

            meetingViewModel.peers.plus(filteredPaginatedPeers)
        } else {
            // Return only Real time peers
            meetingViewModel.peers
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        isLargeRoom = meetingViewModel.hmsSDK.getRoom()?.isLargeRoom == true
        initViewModels()
        return binding.root
    }
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
        participantsUseCase.initRecyclerView(binding.recyclerView)
        binding.closeButton.setOnSingleClickListener {
            closeButton()
        }
        // Search disables conventional updates.
        participantsUseCase.initSearchView(binding.textInputSearch, lifecycleScope)
    }

    private fun closeButton() {
        parentFragmentManager
            .beginTransaction()
            .remove(this)
            .commitAllowingStateLoss()
    }

    private suspend fun initPaginatedPeerListAndIterators(): List<HMSPeer> {
        // Now fetch the first set of peers for all off-stage roles
        val offStageRoleNames =
            meetingViewModel.prebuiltInfoContainer.offStageRoles(meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole?.name!!)
        val deferredList = offStageRoleNames?.filterNotNull()?.map { role ->
            val iterator = meetingViewModel.getPeerlistIterator(role)
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
            roleIteratorDeferred
        }
        // This can throw an exception
        return supervisorScope { deferredList?.awaitAll()?.flatten() ?: emptyList() }
    }

    var updatePeerListJob  : Job? = null
    private fun periodicallyUpdatePeerListForLargeRoom() {
        if(!isLargeRoom) return
        updatePeerListJob?.cancel()
        updatePeerListJob = lifecycleScope.launch {
            while(true) {
                delay(5000) // But we don't want this to overlap with the other ways this happens
                participantsUseCase.updateParticipantsAdapter(getPeerList(true), iteratorMap)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        binding.recyclerView.adapter = participantsUseCase.adapter
        // Initial updating of views
        // Using HMSCoroutine scope here since we want the next call to get queued
//        initPaginatedPeerlist()
        meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                periodicallyUpdatePeerListForLargeRoom() // WARING: This clears the peerlist
                participantsUseCase.updateParticipantsAdapter(getPeerList(), iteratorMap = iteratorMap)
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

    override fun onDestroy() {
        super.onDestroy()
        paginatedPeerList.clear()
        iteratorsInitated = false
        iteratorMap.clear()
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