package live.hms.app2.ui.meeting.participants

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
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


class BottomSheetRoleChangeFragment : BottomSheetDialogFragment(),
    AdapterView.OnItemSelectedListener {
    private val TAG = BottomSheetRoleChangeFragment::class.java.simpleName
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val args: BottomSheetRoleChangeFragmentArgs by navArgs()
    private var stringRole: String? = null

    private var binding by viewLifecycle<LayoutFragmentBottomSheetChangeRoleBinding>()
    private val spinnerRoles by lazy { args.availableRoles }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutFragmentBottomSheetChangeRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initPopup() {
        val peer = meetingViewModel.getPeerForId(args.remotePeerId)

        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.change_role_dialog)
        dialog.findViewById<TextView>(R.id.change_role_text).text =
            "Change the role of ${args.remotePeerName} to"
        val spinner = dialog.findViewById<Spinner>(R.id.role_spinner)
        ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            spinnerRoles
        ).also { arrayAdapter ->
            spinner.adapter = arrayAdapter
            stringRole = arrayAdapter.getItem(0)
            spinner.post { spinner.onItemSelectedListener = this }
        }
        dialog.findViewById<AppCompatButton>(R.id.cancel_btn).setOnClickListener {
            dialog.dismiss()
        }
        val checkBox = dialog.findViewById<CheckBox>(R.id.is_force_update)
        if (peer?.isLocal == true) {
            checkBox.visibility = View.INVISIBLE
        }
        dialog.findViewById<AppCompatButton>(R.id.change_role_btn).apply {
            setOnClickListener {
                val isForceUpdate = checkBox.isChecked.not()

                stringRole?.let {
                    Log.d(TAG, "Selected role: $stringRole")
                    meetingViewModel.changeRole(args.remotePeerId, it, isForceUpdate)
                }
                dialog.dismiss()
                findNavController().popBackStack(R.id.MeetingFragment, false)
            }
        }
        dialog.show()
    }

    private fun initListeners() {
        val peer = meetingViewModel.getPeerForId(args.remotePeerId)


        with(binding) {
            cancel.setOnClickListener { findNavController().popBackStack() }

            promptChangeRole.setOnClickListener {
                initPopup()
            }

            if (peer != null) {
                val audioTrack = peer.audioTrack
                if (audioTrack != null) {
                    setTrackMuteButtonVisibility(
                        audioTrack,
                        peer,
                        muteUnmuteAudio,
                        meetingViewModel.isAllowedToMutePeers(),
                        meetingViewModel.isAllowedToAskUnmutePeers()
                    )
                    muteUnmuteAudio.setOnClickListener {
                        meetingViewModel.togglePeerMute(peer as HMSRemotePeer, HMSTrackType.AUDIO)
                        findNavController().popBackStack()
                    }
                } else {
                    muteUnmuteAudio.visibility = View.GONE
                }

                Log.d(TAG, "Peer video null? ${peer.videoTrack == null}")

                val videoTrack = peer.videoTrack
                if (videoTrack != null) {
                    setTrackMuteButtonVisibility(
                        videoTrack,
                        peer,
                        muteUnmuteVideo,
                        meetingViewModel.isAllowedToMutePeers(),
                        meetingViewModel.isAllowedToAskUnmutePeers()
                    )
                    muteUnmuteVideo.setOnClickListener {
                        meetingViewModel.togglePeerMute(peer as HMSRemotePeer, HMSTrackType.VIDEO)
                        findNavController().popBackStack()
                    }
                } else {
                    muteUnmuteVideo.visibility = View.GONE
                }

                if (meetingViewModel.isAllowedToRemovePeers()) {
                    removePeer.setOnClickListener {
                        meetingViewModel.requestPeerLeave(peer as HMSRemotePeer, "Bye")
                        findNavController().popBackStack()
                    }
                    removePeer.visibility = View.VISIBLE
                } else {
                    removePeer.visibility = View.GONE
                }

                // Self Role UI changes
                if (peer.isLocal) {
//                    promptChangeRole.visibility = View.GONE
                    muteUnmuteVideo.visibility = View.GONE
                    muteUnmuteAudio.visibility = View.GONE
                    removePeer.visibility = View.GONE
                }

            } else {
                muteUnmuteAudio.visibility = View.GONE
                muteUnmuteVideo.visibility = View.GONE
            }
        }
    }

    private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

    private fun setTrackMuteButtonVisibility(
        it: HMSTrack,
        item: HMSPeer,
        button: Button,
        isAllowedToMutePeer: Boolean,
        isAllowedToAskUnmutePeer: Boolean
    ) {
        val isMute = it.isMute
        button.visibility = v(
            !item.isLocal &&
                    (
                            (isAllowedToMutePeer && !isMute) ||
                                    (isAllowedToAskUnmutePeer && isMute)
                            )
        )
        var text = if (isMute) "Unmute" else "Mute"
        text += " " + if (it.type == HMSTrackType.VIDEO) "Video" else "Audio"

        button.text = text
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val stringRole = parent?.adapter?.getItem(position) as String
        Log.d(TAG, "Selected role: $stringRole")
        this.stringRole = stringRole
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

}
