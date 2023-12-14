package live.hms.roomkit.ui.meeting

import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.DialogTrackSelectionBinding
import live.hms.roomkit.util.viewLifecycle
import live.hms.hls_player.HmsHlsPlayer
import live.hms.hls_player.HmsHlsLayer
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.trackTintList


class HlsVideoQualitySelectorBottomSheet(
    private val hlsPlayer: HmsHlsPlayer
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<DialogTrackSelectionBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTrackSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().background_default))

        binding.startConversationTv.setTextColor(
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

        binding.divider.setBackgroundColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.borderDefault,
                HMSPrebuiltTheme.getDefaults().border_bright
            )
        )

        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.closeBtn.setOnClickListener {
            dismiss()
        }
        val allLayers = hlsPlayer.getHmsHlsLayers()
        val currentLayer = hlsPlayer.getCurrentHmsHlsLayer()

        addAutoView(currentLayer == HmsHlsLayer.AUTO)
        allLayers.forEachIndexed { index, layer ->
            when(layer) {
                is HmsHlsLayer.AUTO -> {}
                is HmsHlsLayer.LayerInfo -> {
                    addTrackView("${layer.resolution.height}", index, currentLayer == layer) {
                        hlsPlayer.setHmsHlsLayer(layer)
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    private fun addAutoView(isSelected : Boolean) {
        addTrackView("Auto",-1,isSelected) {
            hlsPlayer.setHmsHlsLayer(HmsHlsLayer.AUTO)
            dismissAllowingStateLoss()
        }
    }

    private fun addTrackView(title: String, index: Int, isSelected : Boolean = false,onSelectedListener: (index : Int) -> Unit) {
        Log.d("TracksView","Trying to add: $title, $index, $isSelected")
        val trackView = LayoutInflater.from(requireContext())
            .inflate(R.layout.track_selection_view, null, false)

        trackView.findViewById<TextView>(R.id.track_label).apply {
            setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
            text = "$title"
        }

        trackView.findViewById<RadioButton>(R.id.track_radio_btn).apply {
            buttonTintList = trackTintList()
            isChecked = isSelected
        }

        trackView.setOnClickListener {
            onSelectedListener.invoke(index)
        }


        binding.trackViewsParent.addView(trackView)
    }
}