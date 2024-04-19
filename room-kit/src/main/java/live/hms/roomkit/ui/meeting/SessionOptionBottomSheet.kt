package live.hms.roomkit.ui.meeting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetOptionBinding
import live.hms.roomkit.ui.GridOptionItem
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.enums.HMSRecordingState

class SessionOptionBottomSheet(
    private val onScreenShareClicked: () -> Unit,
    private val onRecordingClicked: (runnable : Runnable) -> Unit,
    private val onPeerListClicked: () -> Unit,
    private val onBRBClicked: () -> Unit,
    private val onRaiseHandClicked: () -> Unit,
    private val onNameChange: () -> Unit,
    private val showPolls: () -> Unit,
    private val disableHandRaiseDisplay : Boolean = false,
    private val onNoiseClicked : (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetOptionBinding>()
    val gridOptionAdapter = GroupieAdapter()


    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }
        gridOptionAdapter.spanCount = 3
        binding.optionsGrid.apply {
            addItemDecoration(
                InsetItemDecoration(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.backgroundDefault,
                        HMSPrebuiltTheme.getDefaults().background_default
                    ), resources.getDimension(R.dimen.twelve_dp).toInt(), "inset", "inset"
                )
            )
            layoutManager = GridLayoutManager(
                context, gridOptionAdapter.spanCount
            ).apply {
                spanSizeLookup = gridOptionAdapter.spanSizeLookup
            }
            adapter = gridOptionAdapter
        }


        val screenShareOption = GridOptionItem(
            resources.getString(R.string.start_screen_share),
            R.drawable.ic_share_screen,
            {
                dismiss()
                onScreenShareClicked.invoke()
            },
            isSelected = meetingViewModel.isScreenShared()
        )

        val brbOption = GridOptionItem(
            resources.getString(R.string.brb), R.drawable.ic_brb, {
                onBRBClicked.invoke()
                dismiss()
            }, isSelected = meetingViewModel.isBRBOn()
        )

        val raiseHandOption = GridOptionItem(
            resources.getString(R.string.raise_hands), R.drawable.ic_raise_hand, {
                onRaiseHandClicked.invoke()
                dismiss()
            }, isSelected = false
        )

        val captionsButton = GridOptionItem("Show Captions", R.drawable.closed_captions_session_options,
            {
                meetingViewModel.toggleCaptions()
                dismiss()
        }, isSelected = meetingViewModel.captionsEnabledByUser(),
            selectedTitle = "Hide Captions")

        val noiseButton = GridOptionItem("Reduce Noise", R.drawable.reduce_noise_session_option, {
            onNoiseClicked?.invoke()
            dismiss()
        }, isSelected = meetingViewModel.isNoiseCancellationEnabled(),
            selectedTitle = "Noise Reduced")

        val peerListOption = GridOptionItem(
            resources.getString(R.string.peer_list), R.drawable.ic_icon_people, {
                onPeerListClicked.invoke()
                dismiss()
            }, isSelected = false,
        )

        val recordingOption = GridOptionItem(
            resources.getString(R.string.start_record_meeting), R.drawable.ic_record_button_24, {
                onRecordingClicked.invoke(Runnable {

                })

                dismissAllowingStateLoss()
            }, isSelected = false,
        )

        val changeName = GridOptionItem(
            "Change Name", R.drawable.change_name, {
                ChangeNameDialogFragment().show(
                    childFragmentManager,
                    ChangeNameDialogFragment.TAG
                )
            }, isSelected = false
        )

        val videoFilter = GridOptionItem(
            "Video Filter", R.drawable.emoji_icon, {
                onNameChange.invoke()
                dismissAllowingStateLoss()

            }, isSelected = false
        )


        val whiteboard = GridOptionItem(
            resources.getString(R.string.start_white_board), R.drawable.whiteboard, {
                meetingViewModel.toggleWhiteBoard()
                dismissAllowingStateLoss()

            }, isSelected = false
        )




        val group: Group = Section().apply {
            if (meetingViewModel.isParticpantListEnabled())
            add(peerListOption)
            if (meetingViewModel.isWhiteBoardAdmin())
            add(whiteboard)
            if (meetingViewModel.isBRBEnabled())
            add(brbOption)
            if (meetingViewModel.isAllowedToShareScreen())
            add(screenShareOption)
            if (meetingViewModel.displayNoiseCancellationButton()) {
                add(noiseButton)
            }
            if(!disableHandRaiseDisplay && meetingViewModel.handRaiseAvailable()) {
                add(raiseHandOption)
            }
            if (meetingViewModel.isAllowedToBrowserRecord())
            add(recordingOption)
            if(meetingViewModel.areCaptionsAvailable())
                add(captionsButton)
            if(!meetingViewModel.disableNameEdit()) {
                add(changeName)
            }
            if (meetingViewModel.showPollOnUi()) {
                add(GridOptionItem(
                        "Polls and Quizzes", R.drawable.poll_vote, {
                        showPolls.invoke()
                        dismissAllowingStateLoss()
                        }, isSelected = false
                    )
                )
            }
            if (meetingViewModel.isLocalVideoEnabled() == true && meetingViewModel.showVideoFilterIcon()) {
                add(videoFilter)
            }
        }
        gridOptionAdapter.update(listOf(group))



        meetingViewModel.recordingState.observe(viewLifecycleOwner) { state ->

            val isRecording = state in listOf(HMSRecordingState.STARTED, HMSRecordingState.PAUSED,
                HMSRecordingState.RESUMED)

            recordingOption.setSelectedButton(isRecording)
            recordingOption.setText(if (isRecording) resources.getString(R.string.stop_recording) else resources.getString(R.string.start_record_meeting))
        }

        meetingViewModel.isScreenShare.observe(viewLifecycleOwner) {
            screenShareOption.setSelectedButton(it)
            peerListOption.setParticpantCountUpdate(meetingViewModel.peerCount.value)
            screenShareOption.setText(if (it) resources.getString(R.string.stop_share_screen) else resources.getString(R.string.start_screen_share))
        }

        meetingViewModel.showHideWhiteboardObserver.observe(viewLifecycleOwner) {
            whiteboard.setSelectedButton(it.isOpen)
            whiteboard.setText(
                if (it.isOpen && it.isOwner) resources.getString(R.string.stop_white_board)
                else if(it.isOpen.not()) resources.getString(R.string.start_white_board)
                else resources.getString(R.string.stop_white_board)
            )

        }

        meetingViewModel.isHandRaised.observe(viewLifecycleOwner) {
            raiseHandOption.setSelectedButton(it)
        }

        if (meetingViewModel.isParticpantListEnabled())
        meetingViewModel.peerCount.observe(viewLifecycleOwner) {
            peerListOption.setParticpantCountUpdate(it)
        }


        meetingViewModel.recordingState.observe(viewLifecycleOwner) {
            Log.d("SessionOptionBottoSheet", "isRecordingInProgess: $it")
            recordingOption.showProgress(it == HMSRecordingState.STARTING)
        }

    }

    private fun applyTheme() {


        binding.rootLayout.background =
            resources.getDrawable(R.drawable.gray_shape_round_dialog).apply {
                    val color = getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.backgroundDefault,
                        HMSPrebuiltTheme.getDefaults().background_default
                    )
                    setColorFilter(color, PorterDuff.Mode.ADD)
            }


        val borders = arrayOf(
            binding.border5
        )

        borders.forEach {
            it.setBackgroundColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.borderDefault,
                    HMSPrebuiltTheme.getDefaults().border_bright
                )
            )
        }

        binding.title.setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.closeBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

}