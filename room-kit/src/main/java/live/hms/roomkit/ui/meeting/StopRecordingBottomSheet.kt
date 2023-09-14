package live.hms.roomkit.ui.meeting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetStopRecordingBinding
import live.hms.roomkit.databinding.ChangeNameFragmentBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.NameUtils.isValidUserName
import live.hms.roomkit.util.viewLifecycle


class StopRecordingBottomSheet(val onStopRecordingClicked: () -> Unit) : BottomSheetDialogFragment() {


    private var binding by viewLifecycle<BottomSheetStopRecordingBinding>()
    companion object {
        const val TAG = "StopRecordingBottomSheet"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetStopRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        val submitButton = binding.changeName
        val cancelButton = binding.closeBtn

        submitButton.setOnClickListener {
            onStopRecordingClicked()
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismissAllowingStateLoss()
        }

    }

}