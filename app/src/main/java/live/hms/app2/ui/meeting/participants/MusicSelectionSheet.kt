package live.hms.app2.ui.meeting.participants

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.DialogMusicModeChooserBinding
import live.hms.app2.util.viewLifecycle


class MusicSelectionSheet : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<DialogMusicModeChooserBinding>()

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data?.let {
                    binding.tvFileName.text = queryName(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMusicModeChooserBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {

        binding.saveButton.setOnClickListener {
            val selectedView = binding.root.findViewById<RadioButton>(binding.rgMusicModeSelector.checkedRadioButtonId)
            Log.d("selected Mode", "${selectedView.tag}")
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.startButton.setOnClickListener {

        }

        binding.stopButton.setOnClickListener {

        }

        binding.filePicker.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val filePickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerIntent.type = "audio/mpeg"
        resultLauncher.launch(Intent.createChooser(filePickerIntent, "Choose a file"))
    }

    private fun queryName(uri: Uri): String? {
        val returnCursor: Cursor =
            requireActivity().contentResolver.query(uri, null, null, null, null)!!
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

}