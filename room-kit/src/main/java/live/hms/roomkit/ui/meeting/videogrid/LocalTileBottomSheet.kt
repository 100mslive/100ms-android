package live.hms.roomkit.ui.meeting.videogrid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetLocalTileBinding
import live.hms.roomkit.databinding.ExitBottomSheetBinding
import live.hms.roomkit.drawableEnd
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle

class LocalTileBottomSheet(val onMinimizeClicked: () -> Unit, val onNameChange: () -> Unit) : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "LeaveBottomSheet"
    }

    private var binding by viewLifecycle<BottomSheetLocalTileBinding>()

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
        binding = BottomSheetLocalTileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.rootLayout.background = ResourcesCompat.getDrawable(resources,R.drawable.gray_shape_round_dialog, null)!!
            .apply {
                val color = getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.borderDefault,
                    HMSPrebuiltTheme.getDefaults().background_default)
                colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
            }

        var btnArray = arrayOf(
            binding.earpieceBtn, binding.audioOt, binding.changeName
        )

        val borders = arrayOf(
            binding.border5,binding.border6
        )

        binding.roleName.apply {
            setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
        }


        binding.audioOt.text = "${meetingViewModel.hmsSDK.getLocalPeer()?.name} (You)"
        binding.roleName.text = "${meetingViewModel.hmsSDK.getLocalPeer()?.hmsRole?.name.orEmpty()} "
        btnArray.forEach {
            it.setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableEnd?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )

            it.drawableStart?.setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
        }

        borders.forEach {
            it.setBackgroundColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.borderBright,
                    HMSPrebuiltTheme.getDefaults().border_bright
                )
            )
        }


        binding.earpieceBtn.setOnSingleClickListener(200L) {
            onMinimizeClicked.invoke()
            dismissAllowingStateLoss()
        }

        binding.changeName.setOnSingleClickListener(200L) {
            onNameChange.invoke()
            dismissAllowingStateLoss()
        }

    }


}