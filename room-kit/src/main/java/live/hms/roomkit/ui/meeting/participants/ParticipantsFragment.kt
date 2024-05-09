package live.hms.roomkit.ui.meeting.participants

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentParticipantsBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.HMSPeer
const val DIRECTLY_OPENED: String= "PARTICIPANTS_DIRECTLY_OPENED"
class ParticipantsFragment : Fragment() {

    private val TAG = "ParticipantsFragment"
    private var binding by viewLifecycle<FragmentParticipantsBinding>()
    private var alertDialog: AlertDialog? = null
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val participantsUseCase by lazy { ParticipantsUseCase(meetingViewModel, lifecycleScope,
        viewLifecycleOwner
    ) { binding.participantsBack.visibility = View.VISIBLE }
    }

    private val paginatedPeerList = arrayListOf<HMSPeer>()
    private var isLargeRoom = false
    private var iteratorsInitated = false

    override fun onDetach() {
        super.onDetach()
        if(arguments?.getBoolean(DIRECTLY_OPENED) == true) {
            meetingViewModel.restoreTempHiddenCaptions()
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
        binding.participantsBack.setOnClickListener {
            lifecycleScope.launch {
                participantsUseCase.roleFiltering(null)
                binding.participantsBack.visibility = View.GONE
            }
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


    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        binding.recyclerView.adapter = participantsUseCase.adapter
        // Initial updating of views
        // Using HMSCoroutine scope here since we want the next call to get queued
//        initPaginatedPeerlist()
//            meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
//            lifecycleScope.launch {
//                periodicallyUpdatePeerListForLargeRoom() // WARING: This clears the peerlist
//                participantsUseCase.updateParticipantsAdapter(getPeerList(), true)
//            }
//        }
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
        participantsUseCase.clear()
        super.onDestroy()
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