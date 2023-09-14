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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);


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
        newName.setText(meetingViewModel.hmsSDK.getLocalPeer()?.name.orEmpty())
        newName.requestFocus()
        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)

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