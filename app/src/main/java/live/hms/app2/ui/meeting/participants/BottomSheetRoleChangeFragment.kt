package live.hms.app2.ui.meeting.participants

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.LayoutFragmentBottomSheetChangeRoleBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.media.tracks.HMSTrackType
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer


class BottomSheetRoleChangeFragment : BottomSheetDialogFragment(), AdapterView.OnItemSelectedListener {
    private val TAG = BottomSheetRoleChangeFragment::class.java.simpleName
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val args : BottomSheetRoleChangeFragmentArgs by navArgs()
    private var isForce : Boolean? = null

    private var binding by viewLifecycle<LayoutFragmentBottomSheetChangeRoleBinding>()
    private lateinit var popupSpinner : Spinner
    private val spinnerRoles by lazy { args.availableRoles.plus("Cancel") }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutFragmentBottomSheetChangeRoleBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        popupSpinner = view.findViewById(R.id.retroSpinner)
        initPopup()
        initListeners()
    }

    private fun initPopup() {
        ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_list_item_1,
            spinnerRoles
        ).also { arrayAdapter ->
            popupSpinner.adapter = arrayAdapter
            popupSpinner.prompt = "Changing ${args.remotePeerName}'s role to:"
            popupSpinner.setSelection(spinnerRoles.size - 1, false)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            popupSpinner.post { popupSpinner.onItemSelectedListener = this }
        }
    }

    private fun initListeners() {
        with(binding) {
            cancel.setOnClickListener { findNavController().popBackStack() }
            forceChangeRole.setOnClickListener {
                isForce = true
                spinnerDialog()
            }
            promptChangeRole.setOnClickListener {
                isForce = false
                spinnerDialog()
            }

            val peer = meetingViewModel.getPeerForId(args.remotePeerId) as HMSRemotePeer?
            if(peer != null) {

                peer.audioTrack?.let {
                    setTrackMuteButtonVisibility(it, peer, muteUnmuteAudio, meetingViewModel.isAllowedToMutePeers(), meetingViewModel.isAllowedToAskUnmutePeers())
                    muteUnmuteAudio.setOnClickListener { meetingViewModel.togglePeerMute(peer, HMSTrackType.AUDIO) }
                }

                peer.videoTrack?.let {
                    setTrackMuteButtonVisibility(it, peer, muteUnmuteVideo, meetingViewModel.isAllowedToMutePeers(), meetingViewModel.isAllowedToAskUnmutePeers())
                    muteUnmuteVideo.setOnClickListener { meetingViewModel.togglePeerMute(peer, HMSTrackType.VIDEO) }
                }

                if(meetingViewModel.isAllowedToRemovePeers()) {
                    removePeer.setOnClickListener { meetingViewModel.requestPeerLeave(peer, "Bye") }
                    removePeer.visibility = View.VISIBLE
                } else {
                    removePeer.visibility = View.GONE
                }

            } else {
                muteUnmuteAudio.visibility = View.GONE
                muteUnmuteVideo.visibility = View.GONE
            }
        }
    }

    private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

    private fun setTrackMuteButtonVisibility(it: HMSTrack, item: HMSPeer, button : Button, isAllowedToMutePeer : Boolean, isAllowedToAskUnmutePeer : Boolean) {
        val isMute = it.isMute
        button.visibility = v(!item.isLocal &&
                (
                        ( isAllowedToMutePeer && !isMute) ||
                                (isAllowedToAskUnmutePeer && isMute)
                        )
        )
        val text = if(isMute) "Unmute" else "Mute" + " " + if(it.type == HMSTrackType.VIDEO) "Video" else "Audio"

        button.text = text
    }


    private fun spinnerDialog() {
        popupSpinner.performClick()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val stringRole = parent?.adapter?.getItem(position) as String
        Log.d(TAG, "Selected role: $stringRole")
        meetingViewModel.changeRole(args.remotePeerId, stringRole, isForce!!)
        findNavController().popBackStack()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "Nothing selected")
        findNavController().popBackStack()
    }

}
