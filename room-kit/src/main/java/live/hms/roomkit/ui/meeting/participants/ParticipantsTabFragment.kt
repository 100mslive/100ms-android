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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutParticipantsMergeBinding
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer

class ParticipantsTabFragment(val dismissFragment: () -> Unit) : Fragment() {

    private val TAG = "ParticipantsFragment"
    private var binding by viewLifecycle<LayoutParticipantsMergeBinding>()
    private var alertDialog: AlertDialog? = null
    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }
    private val participantsUseCase by lazy { ParticipantsUseCase(meetingViewModel) { meetingViewModel.peers } }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutParticipantsMergeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding.applyTheme()
        initViewModels()
        initOnBackPress()
        initViews()
    }

    private fun updateParticipantCount(count : Int) {
//        binding.participantCount.text = resources.getString(R.string.participants_heading, count)
    }

    private fun initViews() {
        participantsUseCase.initRecyclerView(binding.recyclerView)

        // Search disables conventional updates.
        participantsUseCase.initSearchView(binding.textInputSearch, lifecycleScope)
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        binding.recyclerView.adapter = participantsUseCase.adapter
        // Initial updating of views
        meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                participantsUseCase.updateParticipantsAdapter(meetingViewModel.peers)
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
                    .setTitle(R.string.non_fatal_error_dialog_title)
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
                    dismissFragment()
                }
            })
    }

}