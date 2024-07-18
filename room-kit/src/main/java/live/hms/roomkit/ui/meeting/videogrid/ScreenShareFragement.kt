package live.hms.roomkit.ui.meeting.videogrid

import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.databinding.BottomSheetScreenShareBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.meeting.participants.LoadAfterJoin
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.util.contextSafe
import live.hms.roomkit.util.viewLifecycle
import live.hms.videoview.VideoViewStateChangeListener
import org.webrtc.RendererCommon

const val SCREEN_SHARE_TRACK_ID = "screensharetrackId"
class ScreenShareFragement : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ScreenShareFragement"
    }

    private var swappingOrientation = false

    private var binding by viewLifecycle<BottomSheetScreenShareBinding>()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetScreenShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.isDraggable = false
            }
        }
        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state =
            BottomSheetBehavior.STATE_EXPANDED



        binding.closeBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.rotateBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.rotateBtn.setOnClickListener {
            contextSafe { context, activity ->
                if (swappingOrientation.not()) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                swappingOrientation = !swappingOrientation
            }
        }

        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }
        LoadAfterJoin(meetingViewModel, viewLifecycleOwner) {
            afterJoinAndViewCreated()
        }
    }

    private fun afterJoinAndViewCreated() {

        binding.root.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDefault,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )
        binding.localVideoView.enableZoomAndPan(true)
        binding.localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        binding.localVideoView.addVideoViewStateChangeListener(object : VideoViewStateChangeListener{
            override fun onResolutionChange(newWidth: Int, newHeight: Int) {
                super.onResolutionChange(newWidth, newHeight)
                contextSafe { context, activity ->
                    activity.runOnUiThread {
                        if (newWidth!=0 && newHeight!=0 && newWidth>newHeight) {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                }
            }
        })
        meetingViewModel.tracks.observe(viewLifecycleOwner) { meetingTrack ->

            Log.d(TAG,"Looking for trackId: ${arguments?.getString(SCREEN_SHARE_TRACK_ID)}")
            synchronized(meetingTrack) {
                val track = meetingTrack.find { it.video?.trackId == arguments?.getString(SCREEN_SHARE_TRACK_ID) }?.video
                if (track != null) {
                    binding.localVideoView.addTrack(track)
                } else {
                    dismissAllowingStateLoss()
                }
            }


        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        contextSafe { context, activity ->



            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            swappingOrientation = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()


    }


}