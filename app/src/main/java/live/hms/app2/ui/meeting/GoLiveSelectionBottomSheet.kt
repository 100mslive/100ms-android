package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.GoLiveBottomSheetBinding
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.viewLifecycle


enum class GoLiveOption {
    RTMP, HLS
}

class GoLiveSelectionBottomSheet(
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

        binding.cardHlsView.setOnClickListener {
            optionClickListener.invoke(GoLiveOption.HLS)
            dismiss()
        }

        binding.cardRtmpView.setOnClickListener {
            optionClickListener.invoke(GoLiveOption.RTMP)
            dismiss()
        }
    }
}