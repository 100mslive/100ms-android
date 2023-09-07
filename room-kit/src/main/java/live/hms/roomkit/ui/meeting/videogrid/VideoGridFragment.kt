package live.hms.roomkit.ui.meeting.videogrid

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentGridVideoBinding
import live.hms.roomkit.ui.inset.makeInset
import live.hms.roomkit.ui.meeting.AudioOutputSwitchBottomSheet
import live.hms.roomkit.ui.meeting.ChangeNameDialogFragment
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingFragment
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setIconDisabled
import live.hms.roomkit.util.NameUtils
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.roomkit.util.visibilityOpacity
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import org.webrtc.RendererCommon

class VideoGridFragment : Fragment() {
    companion object {
        private const val TAG = "VideoGridFragment"
    }

    private var binding by viewLifecycle<FragmentGridVideoBinding>()
    private lateinit var settings: SettingsStore

    private lateinit var clipboard: ClipboardManager

    private val meetingViewModel: MeetingViewModel by activityViewModels()

    private lateinit var peerGridVideoAdapter: VideoGridAdapter
    private lateinit var screenShareAdapter: VideoGridAdapter
    var isMinimized = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridVideoBinding.inflate(inflater, container, false)
        settings = SettingsStore(requireContext())

        initVideoGrid()
        initViewModels()
        return binding.root
    }

    private fun initVideoGrid() {
        peerGridVideoAdapter = VideoGridAdapter(this@VideoGridFragment)
        screenShareAdapter = VideoGridAdapter(this@VideoGridFragment, isScreenShare = true)

        binding.viewPagerVideoGrid.apply {
            offscreenPageLimit = 1
            adapter = this@VideoGridFragment.peerGridVideoAdapter

            TabLayoutMediator(binding.tabLayoutDots, this) { _, _ ->
                // No text to be shown
            }.attach()
        }

        binding.viewPagerRemoteScreenShare.apply {
            offscreenPageLimit = 1
            adapter = this@VideoGridFragment.screenShareAdapter
            TabLayoutMediator(binding.tabLayoutDotsRemoteScreenShare, this) { _, _ ->
            }.attach()

        }

        binding.applyTheme()
        binding.iconOption.setOnClickListener {
            LocalTileBottomSheet(onMinimizeClicked = {
                contextSafe { context, activity ->
                    toggleInsetUI(isMinimised = true)
                }
            }, onNameChange = {
                contextSafe { context, activity ->
                    ChangeNameDialogFragment().show(
                        childFragmentManager, ChangeNameDialogFragment.TAG
                    )
                }
            }).show(
                childFragmentManager, VideoGridFragment.TAG
            )
        }

        binding.maximizedIcon.setOnClickListener {
            toggleInsetUI(isMinimised = false)
        }
        binding.insetPill.makeInset {
            isMinimized = isMinimized.not()
            binding.iconOption.visibility = if (isMinimized) View.GONE else View.VISIBLE

        }
        binding.localHmsVideoView?.setZOrderOnTop(true)
        binding.localHmsVideoView?.setZOrderMediaOverlay(true)

        binding.screenShareClose.setOnClickListener {
            meetingViewModel.stopScreenshare(object : HMSActionResultListener {
                override fun onError(error: HMSException) {

                }

                override fun onSuccess() {
                    meetingViewModel.isScreenShare.postValue(false)
                }

            })
        }

        meetingViewModel.peerMetadataNameUpdate.observe(viewLifecycleOwner) { peerTypePair ->
            val isLocal = peerTypePair.first.isLocal
            if (isLocal) {
                when (peerTypePair.second) {
                    HMSPeerUpdate.METADATA_CHANGED -> {
                        val isHandRaised =
                            CustomPeerMetadata.fromJson(peerTypePair.first.metadata)?.isHandRaised == true
                        val isBRB =
                            CustomPeerMetadata.fromJson(peerTypePair.first.metadata)?.isBRBOn == true

                        if (isBRB || isHandRaised) {
                            binding.iconBrb.visibility = View.VISIBLE
                            binding.iconBrb.setImageResource(if (isBRB) R.drawable.ic_brb else R.drawable.raise_hand_modern)
                        } else {
                            binding.iconBrb.visibility = View.GONE
                        }
                    }
                    HMSPeerUpdate.NAME_CHANGED -> {
                        binding.nameInitials.text = NameUtils.getInitials(meetingViewModel.hmsSDK.getLocalPeer()?.name.orEmpty())
                    }
                    // Unused updates
                    HMSPeerUpdate.NETWORK_QUALITY_UPDATED,
                    HMSPeerUpdate.PEER_JOINED,
                    HMSPeerUpdate.PEER_LEFT,
                    HMSPeerUpdate.BECAME_DOMINANT_SPEAKER,
                    HMSPeerUpdate.NO_DOMINANT_SPEAKER,
                    HMSPeerUpdate.ROLE_CHANGED -> {
                    }
                }
            }

        }

        meetingViewModel.activeSpeakers.observe(viewLifecycleOwner) { (video, speakers) ->
            binding.iconAudioLevel.update(speakers.find { it.peer?.isLocal == true }?.level ?: 0)

        }


        meetingViewModel.tracks.observe(viewLifecycleOwner) {
            val localMeeting = it.filter { it.isLocal }.firstOrNull()

            //show or hide inset
            if (it.size == 1 && localMeeting != null) {
                binding.insetPill.visibility = View.GONE
            } else if (it.size > 1 && localMeeting != null) {
                binding.insetPill.visibility = View.VISIBLE
            } else if (localMeeting == null) {
                binding.insetPill.visibility = View.GONE
            }

            localMeeting?.let {


                binding.localHmsVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                if (it.audio?.isMute == true) {
                    if (binding.minimizedIconAudioOff.isEnabled)
                        binding.minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_on_to_off)
                    binding.minimizedIconAudioOff.isEnabled = false
                    binding.iconAudioOff.visibility = View.VISIBLE
                    binding.iconAudioLevel.alpha = visibilityOpacity(false)
                } else {
                    binding.iconAudioOff.visibility = View.INVISIBLE
                    binding.iconAudioLevel.alpha = visibilityOpacity(true)
                    if (binding.minimizedIconAudioOff.isEnabled.not())
                        binding.minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_off_to_on)
                    binding.minimizedIconAudioOff.isEnabled = true
                }

                if (it.video?.isMute == true) {
                    if (binding.minimizedIconVideoOff.isEnabled)
                        binding.minimizedIconVideoOff.setIconDisabled(R.drawable.avd_video_on_to_off)
                    binding.minimizedIconVideoOff.isEnabled = false
                    binding.localHmsVideoView?.visibility = View.INVISIBLE
                    binding.localHmsVideoView?.alpha = 0f
                    binding.nameInitials.text = NameUtils.getInitials(it.peer.name.orEmpty())
                } else {
                    if (binding.minimizedIconVideoOff.isEnabled.not())
                        binding.minimizedIconVideoOff.setIconDisabled(R.drawable.avd_video_off_to_on)
                    binding.minimizedIconVideoOff.isEnabled = true
                    binding.localHmsVideoView?.visibility = View.VISIBLE
                    binding.localHmsVideoView?.alpha = 1f
                    it.video?.let { binding.localHmsVideoView?.addTrack(it) }
                }

            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->

            val screenShareTrackList = tracks.filter { it.isScreen }
            var newRowCount = 0
            var newColumnCount = 0
            //is screen share track is present then reduce the grid and column span else restore
            if (screenShareTrackList.isEmpty()) {
                binding.screenShareContainer.visibility = View.GONE
                newRowCount = 3
                newColumnCount = 2
                binding.divider.setGuidelinePercent(0f)
            } else {
                binding.screenShareContainer.visibility = View.VISIBLE
                newRowCount = 1
                newColumnCount = 2
                binding.divider.setGuidelinePercent(0.75f)
            }

            if (screenShareTrackList.find { it.isLocal } != null) {
                binding.localScreenShareContainer.visibility = View.VISIBLE
            } else {
                binding.localScreenShareContainer.visibility = View.GONE
            }

            meetingViewModel.updateRowAndColumnSpanForVideoPeerGrid.value =
                Pair(newRowCount, newColumnCount)

            val itemsPerPage = newRowCount * newColumnCount
            // Without this, the extra inset adds one more tile than they should
            val tempItems = (tracks.size + itemsPerPage - 1) - 1 // always subtract local peer inset
            val expectedItems = tempItems / itemsPerPage
            screenShareAdapter.totalPages = screenShareTrackList.size
            peerGridVideoAdapter.totalPages = if (expectedItems == 0)
                1
            else expectedItems

            binding.tabLayoutDots.visibility =
                if (peerGridVideoAdapter.itemCount > 1) View.VISIBLE else View.GONE
        }

        meetingViewModel.hmsScreenShareBottomSheetEvent.observe(viewLifecycleOwner) {
            ScreenShareFragement(it).show(
                childFragmentManager, VideoGridFragment.TAG
            )
        }

    }

    private fun toggleInsetUI(isMinimised: Boolean) {
        binding.insetPillMaximised.visibility =
            if (isMinimised) View.GONE else View.VISIBLE
        binding.minimisedInset.visibility =
            if (isMinimised.not()) View.GONE else View.VISIBLE
    }
}