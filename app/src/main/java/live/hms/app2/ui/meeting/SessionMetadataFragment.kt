package live.hms.app2.ui.meeting

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.databinding.SessionMetadataFragmentLayoutBinding

class SessionMetadataFragment : DialogFragment() {
    companion object {
        const val TAG = "SessionMetadataFragment"
    }

    private val meetingViewModel: MeetingViewModel by activityViewModels()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        val binding =
            SessionMetadataFragmentLayoutBinding.inflate(layoutInflater)

        // Set the observed keys
        // Get the current metadat

//
//        val newName = dialoglayout.findViewById<EditText>(R.id.newName)
//        val submitButton = dialoglayout.findViewById<Button>(R.id.submit_name_change_button)
//        val cancelButton = dialoglayout.findViewById<Button>(R.id.cancel_btn)
//
//        submitButton.setOnClickListener {
//            if (NameUtils.isValidUserName(newName)) {
//                val name = newName.text.toString()
//                meetingViewModel.changeName(name)
//                dismiss()
//            }
//        }
//
//        cancelButton.setOnClickListener {
//            dismiss()
//        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}