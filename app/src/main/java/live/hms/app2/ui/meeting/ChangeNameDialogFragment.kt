package live.hms.app2.ui.meeting

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.R
import live.hms.app2.util.NameUtils.isValidUserName


class ChangeNameDialogFragment : DialogFragment() {

    private val meetingViewModel: MeetingViewModel by activityViewModels()

    companion object {
        const val TAG = "ChangeNameDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialoglayout: View =
            layoutInflater.inflate(R.layout.change_name_fragment, null)

        val newName = dialoglayout.findViewById<EditText>(R.id.newName)
        val submitButton = dialoglayout.findViewById<Button>(R.id.submit_name_change_button)
        val cancelButton = dialoglayout.findViewById<Button>(R.id.cancel_btn)

        submitButton.setOnClickListener {
            if (isValidUserName(newName)) {
                val name = newName.text.toString()
                meetingViewModel.changeName(name)
                dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(dialoglayout)
            .create()
    }
}