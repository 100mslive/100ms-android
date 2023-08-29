package live.hms.roomkit.ui.role

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentRolePreviewBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setIconEnabled
import live.hms.roomkit.util.NameUtils
import live.hms.roomkit.util.contextSafe

import live.hms.roomkit.util.viewLifecycle
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.sdk.RolePreviewListener

class RolePreviewFragment : Fragment() {

    private var binding by viewLifecycle<FragmentRolePreviewBinding>()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    private var localAudioTrack: HMSLocalAudioTrack? = null
    private var localVideoTrack: HMSLocalVideoTrack? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRolePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()

        findNavController()

        binding.buttonToggleVideo.setOnClickListener {
            localVideoTrack?.let { videoTrack ->
                if (videoTrack.isMute) {
                    videoTrack.setMute(false)
                    binding.previewView.visibility = View.VISIBLE
                    binding.nameInitials.visibility = View.GONE
                    binding.buttonToggleVideo.setIconEnabled(R.drawable.avd_video_off_to_on)
                } else {
                    binding.previewView.visibility = View.GONE
                    binding.nameInitials.visibility = View.VISIBLE
                    videoTrack.setMute(true)
                    binding.buttonToggleVideo.setIconEnabled(R.drawable.avd_video_on_to_off)
                }
            }
        }


        binding.nameInitials.text =
            NameUtils.getInitials(meetingViewModel.hmsSDK.getLocalPeer()?.name.orEmpty())



        binding.buttonToggleAudio.setOnClickListener {
            localAudioTrack?.let { audioTrack ->
                if (audioTrack.isMute) {
                    audioTrack.setMute(false)
                    binding.buttonToggleVideo.setIconEnabled(R.drawable.avd_mic_off_to_on)
                } else {
                    audioTrack.setMute(true)
                    binding.buttonToggleVideo.setIconEnabled(R.drawable.avd_mic_on_to_off)
                }
            }
        }

        binding.buttonJoinMeeting.setOnClickListener {
            findNavController().navigate(RolePreviewFragmentDirections.actionRolePreviewFragmentToMeetingFragment(false))
        }

        binding.declineButton.setOnClickListener {
            findNavController().popBackStack()
        }

        meetingViewModel.getTrackForRolePendingChangeRequest(object : RolePreviewListener {
            override fun onError(error: HMSException) {
                //TODO add error handling
                contextSafe { context, activity ->
                    Toast.makeText(
                        requireContext(), error.message, Toast.LENGTH_SHORT
                    ).show()
                }
                findNavController().popBackStack()
            }

            override fun onTracks(localTracks: Array<HMSTrack>) {
                contextSafe { context, activity ->
                    activity.runOnUiThread {
                        localTracks.forEach {
                            if (it is HMSLocalVideoTrack) {
                                localVideoTrack = it
                                binding.previewView.addTrack(it)
                                binding.buttonToggleVideo.visibility = View.VISIBLE
                            } else if (it is HMSLocalAudioTrack) {
                                localAudioTrack = it
                                binding.buttonToggleAudio.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }

        })
    }

    override fun onDetach() {
        super.onDetach()
        binding.previewView.removeTrack()
    }


}