package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.vb_prebuilt.SelectedEffect
import live.hms.vb_prebuilt.VirtualBackgroundOptions
import live.hms.video.plugin.video.virtualbackground.VideoPluginMode
import live.hms.videoview.HMSVideoView

class VirtualBackgroundBottomSheet : BottomSheetDialogFragment() {
    private val meetingViewModel: MeetingViewModel by activityViewModels()

    companion object {
        const val TAG = "VirtualBackgroundBottomSheet"
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
            //Don't let the sheet be draggable so that the items will be able to scroll
            sheet.behavior.isDraggable = false
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                val allVbBackgrounds by remember { mutableStateOf(meetingViewModel.vbBackgrounds()) }
                val localPeerMeetingTrack by remember { mutableStateOf(meetingViewModel.getLocalPeerMeetingTracks()) }

                VirtualBackgroundOptions(
                    videoView = { modifier -> AndroidView(modifier = modifier,factory ={
                            HMSVideoView(it).apply {
                                localPeerMeetingTrack?.video?.let { track ->
                                    addTrack(track)
                                }
                    }} ) },
                    allBackgrounds = allVbBackgrounds.backgroundUrls,
                    defaultBackground = meetingViewModel.selectedVbBackgroundUrl,
                    currentlySelectedVbMode = when(meetingViewModel.isVbPlugin) {
                        VideoPluginMode.REPLACE_BACKGROUND -> SelectedEffect.BACKGROUND
                        VideoPluginMode.NONE -> SelectedEffect.NO_EFFECT
                        VideoPluginMode.BLUR_BACKGROUND -> SelectedEffect.BLUR
                    },
                    close = { dismissAllowingStateLoss() },
                    removeEffects = {
                        meetingViewModel.isVbPlugin = VideoPluginMode.NONE
                        meetingViewModel.setupFilterVideoPlugin()},
                    blur = { blurPercentage ->
                        meetingViewModel.isVbPlugin = VideoPluginMode.BLUR_BACKGROUND
                        meetingViewModel.setupFilterVideoPlugin()
                        meetingViewModel.setBlurPercentage(blurPercentage.toInt())
                       },
                    backgroundSelected = { url, bitmap ->
                        meetingViewModel.selectedVbBackgroundUrl = url
                        meetingViewModel.isVbPlugin = VideoPluginMode.REPLACE_BACKGROUND
                        meetingViewModel.setupFilterVideoPlugin(bitmap)
                     },
                    onBlurPercentageChanged = {meetingViewModel.setBlurPercentage(it.toInt())},
                    initialBlurPercentage = 30f
                )
            }
        }
    }
}
