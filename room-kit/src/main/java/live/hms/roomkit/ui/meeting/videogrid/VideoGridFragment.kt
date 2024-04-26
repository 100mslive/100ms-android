package live.hms.roomkit.ui.meeting.videogrid

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEachIndexed
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentGridVideoBinding
import live.hms.roomkit.gone
import live.hms.roomkit.hide
import live.hms.roomkit.initAnimState
import live.hms.roomkit.show
import live.hms.roomkit.startBounceAnimationUpwards
import live.hms.roomkit.ui.inset.makeInset
import live.hms.roomkit.ui.meeting.ChangeNameDialogFragment
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setIconDisabled
import live.hms.roomkit.util.NameUtils
import live.hms.roomkit.util.applyConstraint
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.roomkit.util.visibilityOpacity
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.videoview.HMSVideoView

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
    var whiteboardView : WebView? = null
    var lastVideoMuteState : Boolean? = null

    var localMeeting : MeetingTrack? = null


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
        initWhiteBoard()
        initViewModels()
        return binding.root
    }


    private fun initWhiteBoard() {


        meetingViewModel.debounceWhiteBoardObserver.observe(viewLifecycleOwner) {
            if (it.isOpen) {
                addOrRemoveWebView(shouldAddWebView = true)
                whiteboardView?.show()
                binding.webViewContainer.show()
                val url = it.url
                updateWebViewUrl(url,it.id)

            } else {
                resetWhiteboardState()
                whiteboardView?.hide()
                binding.webViewContainer.hide()
            }
        }

        meetingViewModel.closeWhiteBoard.observe(viewLifecycleOwner, Observer {
            if (it) {
                resetWhiteboardState()
                whiteboardView?.hide()
                meetingViewModel.closeWhiteBoard.value = false
            }
        })

        binding.iconMaximised.setOnClickListener {
            val isFullScreen = meetingViewModel.isWhiteBoardFullScreenMode()
            val toggleMode =  isFullScreen.not()
            meetingViewModel.showWhiteBoardFullScreenSingleLiveEvent.value = toggleMode
            //toggle mode
            meetingViewModel.setWhiteBoardFullScreenMode(toggleMode)
            setWhiteBoardRotation(toggleMode)


        }



    }
    
    private fun resetWhiteboardState() {
        setWhiteBoardRotation(shouldRotate = false)
        meetingViewModel.setWhiteBoardFullScreenMode(false)
        meetingViewModel.showWhiteBoardFullScreenSingleLiveEvent.value = false
        updateWebViewUrl("",null)
    }

    private fun setWhiteBoardRotation(shouldRotate : Boolean) {
        whiteboardView?.let {
            val rotateby90 = shouldRotate && meetingViewModel.isWhiteBoardRotated().not()

            it.updateLayoutParams<ViewGroup.LayoutParams> {
                width  = if(rotateby90) binding.rootLayout.height else ViewGroup.LayoutParams.MATCH_PARENT
                height  = if(rotateby90) binding.rootLayout.width else ViewGroup.LayoutParams.MATCH_PARENT
            }
            it.rotation = if (rotateby90) 90f else 0f
            meetingViewModel.setWhiteBoardRotated(shouldRotate)
        }
    }

    private var lastVisitedId : String? = null
    private fun updateWebViewUrl(url : String,id : String?) {
        if (url.isNullOrEmpty() || id!=lastVisitedId) {
            Log.d("WHITEBOARD", "url loaded  \uD83C\uDF10 ${url}")
            whiteboardView?.loadUrl(url)
            lastVisitedId = id
        }
    }

    override fun onPause() {
        super.onPause()
        if (localMeeting == null) return
        wasLocalVideoTrackVideoOn = (localMeeting?.video?.isMute?:true) == false
        updateVideoViewLayout(binding.insetPillMaximised, isVideoOff = true, localMeeting)
        meetingViewModel.setLocalVideoEnabled(false)
        lastVideoMuteState = true
    }

    override fun onResume() {
        super.onResume()
        if (localMeeting == null) return
        if (wasLocalVideoTrackVideoOn == true) {
            meetingViewModel.setLocalVideoEnabled(true)
            if (isMinimized.not())
            updateVideoViewLayout(binding.insetPillMaximised, isVideoOff = false, localMeeting)
            lastVideoMuteState = false
        }
    }

    private var wasLocalVideoTrackVideoOn : Boolean? =null


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
       // binding.localHmsVideoView?.setZOrderOnTop(true)

//        binding.localHmsVideoView?.setEnableHardwareScaler(true)

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
            binding.iconOption.visibility = if (isMinimized) View.GONE else View.VISIBLE

        }



        meetingViewModel.peerMetadataNameUpdate.observe(viewLifecycleOwner) { peerTypePair ->
            val isLocal = peerTypePair.first.isLocal
            if (isLocal) {
                when (peerTypePair.second) {
                    HMSPeerUpdate.METADATA_CHANGED -> {
                        val isBRB =
                            CustomPeerMetadata.fromJson(peerTypePair.first.metadata)?.isBRBOn == true

                        if (isBRB) {
                            binding.iconBrb.visibility = View.VISIBLE
                            binding.iconBrb.setImageResource(R.drawable.video_tile_brb)
                        } else {
                            binding.iconBrb.visibility = View.GONE
                        }
                    }
                    HMSPeerUpdate.HAND_RAISED_CHANGED -> {
                        if (peerTypePair.first.isHandRaised) {
                            binding.iconBrb.visibility = View.VISIBLE
                            binding.iconBrb.setImageResource(R.drawable.raise_hand_modern)
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
                    HMSPeerUpdate.ROLE_CHANGED,
                        // This is handled in metadata update
                    HMSPeerUpdate.HAND_RAISED_CHANGED -> {
                    }
                }
            }

        }

        meetingViewModel.activeSpeakers.observe(viewLifecycleOwner) { (video, speakers) ->
            binding.iconAudioLevel.update(speakers.find { it.peer?.isLocal == true }?.level ?: 0)

        }
        binding.nameInitials.text = NameUtils.getInitials(meetingViewModel.hmsSDK.getLocalPeer()?.name.orEmpty())


        meetingViewModel.tracks.observe(viewLifecycleOwner) {
            synchronized(it) {
                localMeeting = it.filter { it.isLocal && it.isScreen.not() }.firstOrNull()

                //show or hide inset
                if (meetingViewModel.hasInsetEnabled(meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole)
                        .not()
                )
                    binding.insetPill.visibility = View.GONE
                else if ((it.size == 1 && localMeeting != null)
                    || (it.size == 2 && it.filter { it.isLocal }.size == 2)
                ) {
                    binding.insetPill.visibility = View.GONE
                } else if (it.size > 1 && localMeeting != null) {
                    binding.insetPill.visibility = View.VISIBLE
                } else if (localMeeting == null) {
                    binding.insetPill.visibility = View.GONE
                }

                localMeeting?.let {

                    if (it.audio?.isMute == true) {
                        if (binding.minimizedIconAudioOff.isEnabled)
                            binding.minimizedIconAudioOff.setIconDisabled(
                                R.drawable.avd_mic_on_to_off,
                                R.dimen.two_dp
                            )
                        binding.minimizedIconAudioOff.isEnabled = false
                        binding.iconAudioOff.visibility = View.VISIBLE
                        binding.iconAudioLevel.alpha = visibilityOpacity(false)
                    } else {
                        binding.iconAudioOff.visibility = View.INVISIBLE
                        binding.iconAudioLevel.alpha = visibilityOpacity(true)
                        if (binding.minimizedIconAudioOff.isEnabled.not())
                            binding.minimizedIconAudioOff.setIconDisabled(
                                R.drawable.avd_mic_off_to_on,
                                R.dimen.two_dp
                            )
                        binding.minimizedIconAudioOff.isEnabled = true
                    }

                    if (it.video?.isMute == true) {

                        if (binding.minimizedIconVideoOff.isEnabled)
                            binding.minimizedIconVideoOff.setIconDisabled(
                                R.drawable.avd_video_on_to_off,
                                R.dimen.two_dp
                            )
                        binding.minimizedIconVideoOff.isEnabled = false

                        if (isMinimized.not() && it.video?.isMute !== lastVideoMuteState)
                            updateVideoViewLayout(binding.insetPillMaximised, isVideoOff = true, it)
                        lastVideoMuteState = true
                        binding.nameInitials.text = NameUtils.getInitials(it.peer.name.orEmpty())
                    } else {


                        if (binding.minimizedIconVideoOff.isEnabled.not())
                            binding.minimizedIconVideoOff.setIconDisabled(
                                R.drawable.avd_video_off_to_on,
                                R.dimen.two_dp
                            )
                        binding.minimizedIconVideoOff.isEnabled = true

                        if (isMinimized.not() && it.video?.isMute !== lastVideoMuteState)
                            updateVideoViewLayout(
                                binding.insetPillMaximised,
                                isVideoOff = false,
                                it
                            )
                        lastVideoMuteState = false
                    }

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        super.onDestroyView()
        updateWebViewUrl("",null)

    }
    private fun updateVideoViewLayout(
        insetPillMaximised: ConstraintLayout,
        isVideoOff: Boolean,
        meetingTrack: MeetingTrack?
    ) {

        Log.d(TAG, "updateVideoViewLayout: video on ${isVideoOff.not()} ${meetingTrack?.video?.isMute} visiboilyi ${binding.insetPill.visibility}")

         insetPillMaximised.forEachIndexed { index, view ->
             if (view is HMSVideoView) {
                 insetPillMaximised.removeViewAt(index)
             }
         }

        if (isVideoOff) return
        Log.d(TAG, "updateVideoViewLayout:2 video on ${isVideoOff.not()} ${meetingTrack?.video?.isMute}")

        if (meetingTrack?.video?.isMute == false && meetingTrack.video != null) {
            val hmsVideoView = HMSVideoView(requireContext()).apply {
                setZOrderMediaOverlay(true)
                id = View.generateViewId()
                addTrack(meetingTrack?.video!!)
                initAnimState(alphaOnly = true)
            }

            insetPillMaximised.addView(hmsVideoView)
            hmsVideoView.startBounceAnimationUpwards(offset = 250, interpolator = LinearInterpolator() )
        }

    }

    private  var isWebViewInit : Boolean = false
    private fun addOrRemoveWebView(shouldAddWebView : Boolean) {
        contextSafe { context, activity ->
            val webViewContainer = binding.webViewContainer

            if (shouldAddWebView && isWebViewInit.not()) {
                whiteboardView = WebView(activity).apply {
                    settings?.javaScriptEnabled = true
                    settings?.domStorageEnabled = true
                }
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.gravity = Gravity.CENTER
                webViewContainer.addView(whiteboardView,layoutParams)
                isWebViewInit = true
                webViewContainer.show()
            }
            else if (shouldAddWebView.not()) {
                webViewContainer.hide()
            }
        }
    }

    //Important to prevent redraws like crazy. This was causing flickering issue
    var lastGuideLinePercentage = 0f
    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        meetingViewModel.trackAndWhiteBoardObserver.observe(viewLifecycleOwner) { (whiteBoard ,tracks, isWhiteBoardFullScreen) ->
            if (tracks == null) return@observe

            synchronized(tracks) {
                val screenShareTrackList = tracks.filter { it.isScreen && it.isLocal.not() }
                var newRowCount = 0
                var newColumnCount = 0
                var newGuideLinePercentage = 0f
                val showDockedState = screenShareTrackList.isEmpty().not() || whiteBoard?.isOpen == true
                val hasScreenShareOverriddenWhiteboard = screenShareTrackList.isEmpty().not() && whiteBoard?.isOpen == true

                meetingViewModel.showhasScreenShareOverriddenWhiteboardError(hasScreenShareOverriddenWhiteboard)

                /**
                 * 75% screenshare view port
                 * 100% white board view port (fullscreen)
                 * 75% white board view port
                 */
                newGuideLinePercentage = if (screenShareTrackList.isEmpty().not()) 0.75f
                 else if (whiteBoard?.isOpen == true && isWhiteBoardFullScreen == true) 1.0f
                 else if (whiteBoard?.isOpen == true) 0.75f
                 else 0f

                //is screen share track is present then reduce the grid and column span else restore
                if (showDockedState) {
                    binding.screenShareContainer.show()
                    newRowCount = 1
                    newColumnCount = 2
                } else {
                    binding.screenShareContainer.gone()
                    newRowCount = 3
                    newColumnCount = 2
                }



                //smart updates cause updating evenrything at once would call layout()
                if (lastGuideLinePercentage != newGuideLinePercentage) {
                    if (newGuideLinePercentage == 0.0f) {
                        //un docked state
                        binding.viewPagerVideoGrid.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = ViewGroup.LayoutParams.MATCH_PARENT
                        }

                        binding.rootLayout.applyConstraint {
                            binding.viewPagerVideoGrid.clearTop()
                        }

                    } else {
                        //docked state
                        binding.viewPagerVideoGrid.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                        }
                        binding.rootLayout.applyConstraint {
                            binding.viewPagerVideoGrid.top_toTopOf(binding.divider.id)
                        }
                    }

                    binding.divider.setGuidelinePercent(newGuideLinePercentage)
                    lastGuideLinePercentage = newGuideLinePercentage
                }

                if (screenShareTrackList.size > 1 && hasScreenShareOverriddenWhiteboard) {
                    binding.tabLayoutDotsRemoteScreenShare.show()
                } else {
                    binding.tabLayoutDotsRemoteScreenShare.gone()
                }


                meetingViewModel.updateRowAndColumnSpanForVideoPeerGrid.value =
                    Pair(newRowCount, newColumnCount)

                val itemsPerPage = newRowCount * newColumnCount


                val remoteScreenShareTilesCount = screenShareTrackList.size
                val localScreenShareTileCount = tracks.filter { it.isLocal && it.isScreen }.size
                val hasLocalTile = tracks.filter { it.isLocal && it.isScreen.not() }.size == 1
                val bothLocalTile = (tracks.size == 2 && tracks.filter { it.isLocal }.size == 2)
                val hasRemotePeers = tracks.filter { it.isLocal.not() }.isNotEmpty()
                val hasInsetTileVisible =
                    meetingViewModel.hasInsetEnabled(meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole) && hasLocalTile && bothLocalTile.not() && hasRemotePeers
                val onthePeerGridTileCount =
                    tracks.size - remoteScreenShareTilesCount - localScreenShareTileCount + (1 * if (hasInsetTileVisible) -1 else 0)
                // Without this, the extra inset adds one more tile than they should
                val expectedPages =
                    Math.ceil((onthePeerGridTileCount.toDouble() / itemsPerPage.toDouble())).toInt()
                screenShareAdapter.totalPages = remoteScreenShareTilesCount
                meetingViewModel.transcriptionsPositionUseCase.setScreenShare(remoteScreenShareTilesCount + localScreenShareTileCount != 0)
                peerGridVideoAdapter.totalPages = expectedPages

                binding.tabLayoutDots.visibility =
                    if (peerGridVideoAdapter.itemCount > 1) View.VISIBLE else View.GONE
            }
        }

        meetingViewModel.hmsScreenShareBottomSheetEvent.observe(viewLifecycleOwner) {
            val args = Bundle()
                .apply {
                    putString(SCREEN_SHARE_TRACK_ID, it)
                }

            ScreenShareFragement().apply { this.arguments = args }.show(
                childFragmentManager, VideoGridFragment.TAG
            )
        }

    }

    private fun toggleInsetUI(isMinimised: Boolean) {

        this.isMinimized = isMinimised
        binding.insetPillMaximised.visibility =
            if (isMinimised) View.GONE else View.VISIBLE

        binding.minimisedInset.visibility =
            if (isMinimised.not()) View.GONE else View.VISIBLE

        if (isMinimised) {
            updateVideoViewLayout(binding.insetPillMaximised, isVideoOff = true, localMeeting)
        } else {
            updateVideoViewLayout(binding.insetPillMaximised, isVideoOff = false, localMeeting)
        }
    }


}