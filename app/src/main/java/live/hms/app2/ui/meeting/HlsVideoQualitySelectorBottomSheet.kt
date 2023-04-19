package live.hms.app2.ui.meeting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.DialogTrackSelectionBinding
import live.hms.app2.util.viewLifecycle
import live.hms.hls_player.HmsHlsPlayer
import live.hms.hls_player.HmsHlsLayer


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
                        dismiss()
                    }
                }
            }
        }
    }

    private fun addAutoView(isSelected : Boolean) {
        addTrackView("Auto",-1,isSelected) {
            hlsPlayer.setHmsHlsLayer(HmsHlsLayer.AUTO)
            dismiss()
        }
    }

    private fun addTrackView(title: String, index: Int, isSelected : Boolean = false,onSelectedListener: (index : Int) -> Unit) {
        Log.d("TracksView","Trying to add: $title, $index, $isSelected")
        val trackView = LayoutInflater.from(requireContext())
            .inflate(R.layout.track_selection_view, null, false)

        trackView.findViewById<TextView>(R.id.track_label).text = "$title"
        trackView.findViewById<RadioButton>(R.id.track_radio_btn).isChecked = isSelected

        trackView.setOnClickListener {
            onSelectedListener.invoke(index)
        }
        binding.trackViewsParent.addView(trackView)
    }
}