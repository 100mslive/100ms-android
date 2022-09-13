package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ExpandableListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.SettingsBottomSheetDialogBinding
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle


class SettingsBottomSheet(
    private val meetingViewModel: MeetingViewModel,
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

        binding.backBtn.setOnClickListener {
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

        binding.btnMeetingMode.apply {
            binding.audioModeSwitch.isChecked = meetingViewModel.getCurrentMediaModeCheckedState()
            updateMeetingAudioMode()
            setOnSingleClickListener {
                binding.audioModeSwitch.callOnClick()
            }
        }

        binding.audioModeSwitch.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                meetingViewModel.toggleMediaMode()
                updateMeetingAudioMode()
            }
        })

        binding.btnBrb.apply {

            if (meetingViewModel.isBRBOn()) {
                binding.tvBrbStatus.text = "Disable BRB"
            } else {
                binding.tvBrbStatus.text = "Enable BRB"
            }

            setOnSingleClickListener(350) {
                meetingViewModel.toggleBRB()
                dismiss()
            }
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

        binding.btnShowStats.apply {
            if (meetingViewModel.statsToggleLiveData.value == true) {
                binding.tvStats.text = "Hide Stats"
            } else {
                binding.tvStats.text = "Show Stats"
            }
            setOnSingleClickListener(350) {

                meetingViewModel.statsToggleData.postValue(meetingViewModel.statsToggleLiveData.value?.not())
                dismiss()
            }
        }
    }

    fun updateMeetingAudioMode(){
        if (meetingViewModel.getCurrentMediaModeCheckedState()) {
            binding.tvAudioMode.text = "Audio Mode : Media"
        } else {
            binding.tvAudioMode.text = "Audio Mode : In Call"
        }
    }

    private fun setupConfig(){
        if (meetingViewModel.hmsSDK.getLocalPeer()?.isWebrtcPeer()?.not() == true) {
            binding.layoutExpandableList.visibility = View.GONE
            binding.btnCameraSwitch.visibility = View.GONE
            binding.btnShowStats.visibility = View.GONE
            binding.btnBrb.visibility = View.GONE
        }
    }
}