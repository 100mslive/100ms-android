package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.databinding.BottomSheetOptionBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setDrawables
import live.hms.roomkit.ui.GridOptionItem
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.audio.HMSAudioManager

class GridOptionBottomSheet(
    private val onScreenShareClicked: () -> Unit,
    private val onRecordingClicked: () -> Unit,
    private val onPeerListClicked: () -> Unit,
    private val onBRBClicked: () -> Unit,
    private val onRaiseHandClicked: () -> Unit,
    private val onNameChange: () -> Unit,
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()

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
                    ), resources.getDimension(R.dimen.four_dp).toInt(), "inset", "inset"
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
            getString(R.string.screen_share),
            R.drawable.ic_share_screen,
            onScreenShareClicked,
            isSelected = meetingViewModel.isScreenShared()
        )

        val brbOption = GridOptionItem(
            getString(R.string.brb), R.drawable.ic_brb, {
                onBRBClicked.invoke()
                dismiss()
            }, isSelected = meetingViewModel.isBRBOn()
        )

        val raiseHandOption = GridOptionItem(
            getString(R.string.raise_hand), R.drawable.ic_raise_hand, {
                onRaiseHandClicked.invoke()
                dismiss()
            }, isSelected = false
        )

        val peerListOption = GridOptionItem(
            getString(R.string.peer_list), R.drawable.ic_icon_people, {
                onPeerListClicked.invoke()
                dismiss()
            }, isSelected = false,
        )

        val nameChangeOption = GridOptionItem(
            getString(R.string.change_name), R.drawable.person_icon, {
                onNameChange.invoke()
                dismiss()
            }, isSelected = false,
        )


        val group: Group = Section().apply {
            add(peerListOption)
            add(brbOption)
            add(screenShareOption)
            add(raiseHandOption)
            add(nameChangeOption)
        }
        gridOptionAdapter.update(listOf(group))

        meetingViewModel.isScreenShare.observe(viewLifecycleOwner) {
            screenShareOption.setSelectedButton(it)
            peerListOption.setParticpantCountUpdate(meetingViewModel.peers.size)
        }

        meetingViewModel.isHandRaised.observe(viewLifecycleOwner) {
            raiseHandOption.setSelectedButton(it)
        }

        meetingViewModel.previewRoomStateLiveData.observe(viewLifecycleOwner) {
            peerListOption.setParticpantCountUpdate(it.second.peerCount)
        }


    }

    private fun applyTheme() {
        binding.root.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDefault,
                HMSPrebuiltTheme.getDefaults().background_default
            )
        )

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