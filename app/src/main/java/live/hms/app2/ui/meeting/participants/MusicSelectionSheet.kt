package live.hms.app2.ui.meeting.participants

import android.R.attr.data
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.loader.content.CursorLoader
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.DialogMusicModeChooserBinding
import live.hms.app2.util.MediaPlayerManager
import live.hms.app2.util.contextSafe
import live.hms.app2.util.getName
import live.hms.app2.util.viewLifecycle


class MusicSelectionSheet : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<DialogMusicModeChooserBinding>()

    private val mediaPlayerManager by lazy {
        MediaPlayerManager(lifecycle)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    contextSafe { context, activity ->

                        binding.tvFileName.apply {
                            isSelected = true
                            text = uri.getName(requireContext()).orEmpty()
                            visibility = View.VISIBLE
                        }

                        mediaPlayerManager.startPlay(
                            uri,
                            context.applicationContext
                        )
                    }

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
            val selectedView =
                binding.root.findViewById<RadioButton>(binding.rgMusicModeSelector.checkedRadioButtonId)
            Log.d("selected Mode", "${selectedView.tag}")
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.startButton.setOnClickListener {
            mediaPlayerManager.resume()
        }

        binding.stopButton.setOnClickListener {
            mediaPlayerManager.pause()
        }

        binding.filePicker.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val filePickerIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )
        resultLauncher.launch(filePickerIntent)
    }

}