package live.hms.android100ms.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.android100ms.databinding.ModalMeetingBottomSheetBinding
import live.hms.android100ms.util.viewLifecycle

class MeetingOptionsBottomSheet : BottomSheetDialogFragment() {
    private var binding by viewLifecycle<ModalMeetingBottomSheetBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ModalMeetingBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
}