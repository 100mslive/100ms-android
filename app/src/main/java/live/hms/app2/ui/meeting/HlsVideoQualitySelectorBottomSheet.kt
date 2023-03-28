package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.media3.common.Player
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.DialogTrackSelectionBinding
import live.hms.app2.util.viewLifecycle


class HlsVideoQualitySelectorBottomSheet(
    private val player: Player
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

        val trackGroupInfo = player.currentTracksInfo.trackGroupInfos.find { it.trackType == C.TRACK_TYPE_VIDEO }
        val selected = player.trackSelectionParameters.trackSelectionOverrides.getOverride(trackGroupInfo!!.trackGroup)
        val selectedIndex = selected?.trackIndices?.get(0)

        binding.trackViewsParent.removeAllViews()
        addAutoView(selectedIndex == null)
        player.currentTracksInfo.trackGroupInfos.find {
            it.trackType == C.TRACK_TYPE_VIDEO
        }?.also { trackGroupInfo ->
            for (index in 0 until trackGroupInfo.trackGroup.length) {
                if (trackGroupInfo.trackType == C.TRACK_TYPE_VIDEO) {
                    val format = trackGroupInfo.trackGroup.getFormat(index)
                    addTrackView("${format.height}p", format.id?.toInt() ?: 0,selectedIndex == index) {
                        val trackSelectionOverrides = TrackSelectionOverrides.Builder()
                        trackSelectionOverrides.addOverride(
                            TrackSelectionOverrides.TrackSelectionOverride(
                                trackGroupInfo.trackGroup,
                                listOf(
                                    trackGroupInfo.trackGroup.getFormat(index).id?.toInt() ?: 0
                                )
                            )
                        )
                        player.trackSelectionParameters =
                            player.trackSelectionParameters.buildUpon()
                                .setTrackSelectionOverrides(trackSelectionOverrides.build())
                                .build()
                        dismiss()
                    }
                }
            }
        }

    }

    private fun addAutoView(isSelected : Boolean) {
        addTrackView("Auto",-1,isSelected) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackSelectionOverrides(TrackSelectionOverrides.EMPTY)
                .build()
            dismiss()
        }
    }

    private fun addTrackView(title: String, index: Int, isSelected : Boolean = false,onSelectedListener: (index: Int) -> Unit) {
        val trackView = LayoutInflater.from(requireContext())
            .inflate(R.layout.track_selection_view, null, false)

        trackView.findViewById<TextView>(R.id.track_label).text = "${title}"
        trackView.findViewById<RadioButton>(R.id.track_radio_btn).isChecked = isSelected

        trackView.setOnClickListener {
            onSelectedListener.invoke(index)
        }
        binding.trackViewsParent.addView(trackView)
    }
}