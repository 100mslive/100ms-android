package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ExpandableListAdapter
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.SettingsBottomSheetDialogBinding
import live.hms.app2.ui.home.MeetingLinkFragmentDirections
import live.hms.app2.ui.meeting.participants.MusicSelectionSheet
import live.hms.app2.ui.meeting.participants.ParticipantsFragment
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle


class SettingsBottomSheet(
    private val meetingViewModel: MeetingViewModel,
    private val participantsListener : ()->Unit
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<SettingsBottomSheetDialogBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupConfig()
        val settingsExpandableListAdapter: ExpandableListAdapter =
            SettingsExpandableListAdapter(requireContext())
        binding.layoutExpandableList.setAdapter(settingsExpandableListAdapter)
        binding.layoutExpandableList.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            when (SettingsExpandableListAdapter.MeetingLayout.valueOf(
                settingsExpandableListAdapter.getChild(
                    groupPosition,
                    childPosition
                ).toString()
            )) {
                SettingsExpandableListAdapter.MeetingLayout.GridView -> {
                    meetingViewModel.setMeetingViewMode(MeetingViewMode.GRID)
                    dismiss()
                }
                SettingsExpandableListAdapter.MeetingLayout.ActiveSpeaker -> {
                    meetingViewModel.setMeetingViewMode(MeetingViewMode.ACTIVE_SPEAKER)
                    dismiss()
                }
                SettingsExpandableListAdapter.MeetingLayout.HeroMode -> {
                    meetingViewModel.setMeetingViewMode(MeetingViewMode.PINNED)
                    dismiss()
                }
            }
            false
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        binding.btnDeviceSettings.apply {
            setOnSingleClickListener {
                val audioSwitchBottomSheet =
                    AudioOutputSwitchBottomSheet(meetingViewModel) { audioDevice, isMuted ->
                        dismiss()
                    }
                audioSwitchBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    MeetingFragment.AudioSwitchBottomSheetTAG
                )
            }
        }

        binding.participantCount.text = meetingViewModel.hmsSDK.getPeers().size.toString()
        binding.layoutParticipants.apply {
            setOnSingleClickListener {
                dismiss()
                participantsListener.invoke()
            }
        }

        binding.btnPipMode.apply {
            setOnSingleClickListener {
                dismiss()
                requireActivity().enterPictureInPictureMode()
            }
        }

        binding.btnMeetingMode.apply {
            binding.audioModeSwitch.isChecked = meetingViewModel.getCurrentMediaModeCheckedState()
            updateMeetingAudioMode()
            setOnSingleClickListener {
                binding.audioModeSwitch.callOnClick()
            }
        }

        binding.audioModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            meetingViewModel.toggleMediaMode()
            updateMeetingAudioMode()
        }

        binding.brbSwitch.isChecked = meetingViewModel.isBRBOn()

        binding.brbSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            meetingViewModel.toggleBRB()
        }


        binding.btnChangeName.apply {
            setOnSingleClickListener(350) {
                ChangeNameDialogFragment().show(
                    childFragmentManager,
                    ChangeNameDialogFragment.TAG
                )
            }
        }

        binding.btnCameraSwitch.apply {
            setOnSingleClickListener(350) {
                meetingViewModel.flipCamera()
                dismiss()
            }
        }

        binding.btnAudioShare.apply {
            setOnSingleClickListener(350) {
                val musicSelectionSheet = MusicSelectionSheet()
                musicSelectionSheet.show(requireActivity().supportFragmentManager,"musicSelectionSheet")
                dismiss()
            }
        }

        binding.showStatsSwitch.isChecked = meetingViewModel.statsToggleLiveData.value == true

        binding.showStatsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            meetingViewModel.statsToggleData.postValue(isChecked)
        }

    }

    fun updateMeetingAudioMode() {
        if (meetingViewModel.getCurrentMediaModeCheckedState()) {
            binding.tvAudioMode.text = "Audio Mode : Media"
        } else {
            binding.tvAudioMode.text = "Audio Mode : In Call"
        }
    }

    private fun setupConfig() {
        if (meetingViewModel.hmsSDK.getLocalPeer()?.isWebrtcPeer()?.not() == true) {
            binding.layoutExpandableList.visibility = View.GONE
            binding.btnCameraSwitch.visibility = View.GONE
            binding.btnAudioShare.visibility = View.GONE
            binding.btnShowStats.visibility = View.GONE
            binding.btnBrb.visibility = View.GONE
        }
    }
}