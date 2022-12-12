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


class SendMetaDataDialogFragment : DialogFragment() {

    private val meetingViewModel: MeetingViewModel by activityViewModels()

    companion object {
        const val TAG = "ChangeNameDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialoglayout: View = layoutInflater.inflate(R.layout.send_hls_metadata_fragment, null)

        val payload = dialoglayout.findViewById<EditText>(R.id.payload)
        val duration = dialoglayout.findViewById<EditText>(R.id.duration)
        val submitButton = dialoglayout.findViewById<Button>(R.id.submit_metadata_request)
        val cancelButton = dialoglayout.findViewById<Button>(R.id.cancel_btn)

        submitButton.setOnClickListener {
            val payloadText = payload.text.toString()
            val durationText = duration.text.toString().toLongOrNull() ?: 0
            if (payload.text.isNotEmpty() && durationText > 0) {
                meetingViewModel.sendHlsMetadata(
                    metaDataModel = live.hms.video.sdk.models.MetaDataModel(
                        payloadText,
                        durationText,
                        ""
                    )
                )
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