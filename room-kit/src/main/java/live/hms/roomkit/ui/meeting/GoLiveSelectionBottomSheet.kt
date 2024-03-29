package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.databinding.GoLiveBottomSheetBinding
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.viewLifecycle


enum class GoLiveOption {
    RTMP, HLS
}

class GoLiveSelectionBottomSheet(
    val isAllowedToHlsStream : Boolean, val isAllowedToRtmpStream: Boolean,
    private val optionClickListener: (goLiveOption: GoLiveOption) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var settingsStore : SettingsStore

    private var binding by viewLifecycle<GoLiveBottomSheetBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GoLiveBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsStore = SettingsStore(requireContext())
        binding.tvStartStreamingTitle.text = "Welcome ${settingsStore.username}!"

        binding.closeBtn.setOnClickListener {
            dismiss()
        }

        if (isAllowedToHlsStream){
            binding.cardHlsView.visibility = View.VISIBLE
            binding.cardHlsView.setOnClickListener {
                optionClickListener.invoke(GoLiveOption.HLS)
                dismiss()
            }
        }

        if (isAllowedToRtmpStream){
            binding.cardRtmpView.visibility = View.VISIBLE
            binding.cardRtmpView.setOnClickListener {
                optionClickListener.invoke(GoLiveOption.RTMP)
                dismiss()
            }
        }
    }
}