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
import live.hms.vb_prebuilt.VirtualBackgroundOptions
import live.hms.video.plugin.video.virtualbackground.VideoPluginMode
import live.hms.videoview.HMSVideoView

class VirtualBackgroundBottomSheet : BottomSheetDialogFragment() {
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val previewViewModel : PreviewViewModel by activityViewModels()

    companion object {
        const val TAG = "VirtualBackgroundBottomSheet"
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let {
            val sheet = it as BottomSheetDialog
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
                VirtualBackgroundOptions(
                    videoView = { modifier -> AndroidView(modifier = modifier,factory ={
                            HMSVideoView(it).apply {
                                previewViewModel.track?.video?.let { track ->
                                    addTrack(track)
                                }
                    }} ) },
                    allBackgrounds = allVbBackgrounds.backgroundUrls,
                    defaultBackground = allVbBackgrounds.default,
                    close = { dismissAllowingStateLoss() },
                    removeEffects = {meetingViewModel.isVbPlugin = VideoPluginMode.NONE
                        meetingViewModel.setupFilterVideoPlugin()},
                    blur = { blurPercentage ->
                        meetingViewModel.isVbPlugin = VideoPluginMode.BLUR_BACKGROUND
                        meetingViewModel.setupFilterVideoPlugin()
                        meetingViewModel.setBlurPercentage(blurPercentage.toInt())
                       },
                    backgroundSelected = {
                        meetingViewModel.isVbPlugin = VideoPluginMode.REPLACE_BACKGROUND
                        meetingViewModel.setupFilterVideoPlugin(it)
                     },
                    onBlurPercentageChanged = {meetingViewModel.setBlurPercentage(it.toInt())},
                    initialBlurPercentage = 30f
                )
            }
        }
    }
}
