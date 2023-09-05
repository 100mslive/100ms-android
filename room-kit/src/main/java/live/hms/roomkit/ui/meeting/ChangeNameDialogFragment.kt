package live.hms.roomkit.ui.meeting

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetLocalTileBinding
import live.hms.roomkit.databinding.ChangeNameFragmentBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.NameUtils.isValidUserName
import live.hms.roomkit.util.viewLifecycle


class ChangeNameDialogFragment : BottomSheetDialogFragment() {

    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private var binding by viewLifecycle<ChangeNameFragmentBinding>()
    companion object {
        const val TAG = "ChangeNameDialogFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ChangeNameFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        val newName = binding.newName
        val submitButton = binding.changeName
        val cancelButton = binding.closeBtn

        submitButton.setOnClickListener {
            if (isValidUserName(newName)) {
                val name = newName.text.toString()
                meetingViewModel.changeName(name)
                dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dismissAllowingStateLoss()
        }

    }

}