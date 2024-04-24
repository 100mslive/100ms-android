package live.hms.roomkit.ui.meeting

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.SettingsBottomSheetDialogBinding
import live.hms.roomkit.ui.meeting.participants.MusicSelectionSheet
import live.hms.roomkit.util.setOnSingleClickListener
import live.hms.roomkit.util.viewLifecycle


class SettingsBottomSheet(
    private val meetingViewModel: MeetingViewModel,
    private val participantsListener: () -> Unit,
    private val openBulkRoleChange : () -> Unit,
    private val openPolls : () -> Unit
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<SettingsBottomSheetDialogBinding>()

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        meetingViewModel.restoreTempHiddenCaptions()
    }
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
            SettingsExpandableListAdapter(requireContext(), meetingViewModel.isPrebuiltDebugMode())
        binding.layoutExpandableList.setAdapter(settingsExpandableListAdapter)
        binding.layoutExpandableList.setOnGroupClickListener(ExpandableListView.OnGroupClickListener { parent, v, groupPosition, id ->
            setListViewHeight()
            false
        })

        binding.layoutExpandableList.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            setListViewHeight()
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
                    AudioOutputSwitchBottomSheet { audioDevice, isMuted ->
                        dismiss()
                    }
                audioSwitchBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    MeetingFragment.AudioSwitchBottomSheetTAG
                )
            }
        }

        binding.btnMetaDataSend.apply {
            setOnSingleClickListener(350) {
                SendMetaDataDialogFragment().show(
                    childFragmentManager,
                    ChangeNameDialogFragment.TAG
                )
            }
        }

        binding.btnChangeMetadata.apply {
            setOnSingleClickListener(350) {
                SessionMetadataFragment().show(
                    childFragmentManager,
                    SessionMetadataFragment.TAG
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

        binding.btnBulkRoleChange.apply {
            setOnSingleClickListener {
                dismissAllowingStateLoss()
                openBulkRoleChange()
            }
            visibility = if(meetingViewModel.isAllowedToChangeRole() && meetingViewModel.isPrebuiltDebugMode()) View.VISIBLE else View.GONE
        }
        if(meetingViewModel.isPrebuiltDebugMode().not() || meetingViewModel.isAllowedToCreatePolls().not()) {
            binding.btnPolls.visibility = View.GONE
        }
        binding.btnPolls.apply {
            setOnSingleClickListener{
                dismiss()
                openPolls()
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

        binding.remoteMuteAll.apply {
            binding.audioModeSwitch.isChecked = meetingViewModel.getCurrentMediaModeCheckedState()
            updateMeetingAudioMode()
            setOnSingleClickListener {
                binding.audioModeSwitch.callOnClick()
            }
        }

        val isAllowedToMuteUnmute =
            meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers()
        var remotePeersAreMute: Boolean? = null
        if (isAllowedToMuteUnmute) {
            remotePeersAreMute = meetingViewModel.areAllRemotePeersMute()
        }

        binding.remoteMuteAll.apply {
            if (meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers() && isAllowedToMuteUnmute && meetingViewModel.isPrebuiltDebugMode()) {
                visibility = View.VISIBLE
            }

            setOnClickListener {

                if (remotePeersAreMute == null) {
                    Toast.makeText(
                        requireContext(),
                        "No remote peers, or their audio tracks are absent",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // If they exist and have a mute status, reverse it.
                    meetingViewModel.remoteMute(!remotePeersAreMute, null)
                }
                true
            }
        }

        binding.remoteMuteRole.apply {
            if (meetingViewModel.isAllowedToMutePeers() && meetingViewModel.isAllowedToAskUnmutePeers() && isAllowedToMuteUnmute && meetingViewModel.isPrebuiltDebugMode()) {
                visibility = View.VISIBLE
            }
            setOnSingleClickListener {
                dismiss()
                val availableRoles = meetingViewModel.getAvailableRoles().map { it.name }
                var stringRole: String = availableRoles[0]

                val dialog = Dialog(requireActivity())
                dialog.setContentView(R.layout.unmute_based_on_role_dialog)
                val spinner = dialog.findViewById<Spinner>(R.id.role_spinner)
                ArrayAdapter(
                    requireActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    availableRoles
                ).also { arrayAdapter ->
                    spinner.adapter = arrayAdapter
                    spinner.post {
                        spinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    stringRole = parent?.adapter?.getItem(position) as String
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                    }
                }
                dialog.findViewById<AppCompatButton>(R.id.cancel_btn).setOnClickListener {
                    dialog.dismiss()
                }

                dialog.findViewById<AppCompatButton>(R.id.change_role_btn).apply {
                    setOnClickListener {
                        stringRole.let {
                            meetingViewModel.remoteMute(
                                remotePeersAreMute?.not() == true,
                                listOf(stringRole)
                            )
                        }
                        dialog.dismiss()
                    }
                }
                dialog.show()

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
                musicSelectionSheet.show(
                    requireActivity().supportFragmentManager,
                    "musicSelectionSheet"
                )
                dismiss()
            }
        }

        binding.showStatsSwitch.isChecked = meetingViewModel.statsToggleLiveData.value == true

        binding.showStatsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            meetingViewModel.setRtcObserver(isChecked)
        }

    }

    private fun updateMeetingAudioMode() {
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
            binding.btnBrb.visibility = View.GONE
            binding.btnMetaDataSend.visibility = View.GONE
            binding.remoteMuteAll.visibility = View.GONE
            binding.remoteMuteRole.visibility = View.GONE
        }

        if (meetingViewModel.isPrebuiltDebugMode().not()) {
            //
            binding.btnShowStats.visibility = View.GONE
            binding.btnMeetingMode.visibility = View.GONE
            binding.btnMetaDataSend.visibility = View.GONE
            binding.remoteMuteAll.visibility = View.GONE
            binding.remoteMuteRole.visibility = View.GONE
        }
    }

    private fun setListViewHeight() {
        val listAdapter: ExpandableListAdapter = binding.layoutExpandableList.expandableListAdapter
        var totalHeight = 0;
        val desiredWidth = View.MeasureSpec.makeMeasureSpec(
            binding.layoutExpandableList.getWidth(),
            View.MeasureSpec.EXACTLY
        );
        for (i in 0 until listAdapter.groupCount) {
            val groupItem = listAdapter.getGroupView(0, false, null, binding.layoutExpandableList);
            groupItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);

            totalHeight += groupItem.measuredHeight;

            if (((binding.layoutExpandableList.isGroupExpanded(i).not()))
            ) {
                for (j in 0 until listAdapter.getChildrenCount(i)) {
                    val listItem = listAdapter.getChildView(
                        i, j, false, null,
                        binding.layoutExpandableList
                    )
                    listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);

                    totalHeight += listItem.measuredHeight;

                }
            }
        }
        val params: ViewGroup.LayoutParams = binding.layoutExpandableList.layoutParams
        var height: Int = (totalHeight
                + binding.layoutExpandableList.dividerHeight * (listAdapter.groupCount - 1))
        if (height < 10) height = 200
        params.height = height
        binding.layoutExpandableList.layoutParams = params
        binding.layoutExpandableList.requestLayout()
    }
}