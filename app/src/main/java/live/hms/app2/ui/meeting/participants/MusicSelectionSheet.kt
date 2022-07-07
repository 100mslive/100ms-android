package live.hms.app2.ui.meeting.participants

import android.R.attr.data
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.loader.content.CursorLoader
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.DialogMusicModeChooserBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.meeting.MeetingViewModelFactory
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.enums.AudioMixingMode


class MusicSelectionSheet : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<DialogMusicModeChooserBinding>()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application,
            requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
        )
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                data?.let {
                    meetingViewModel.startAudioshare(it,
                        getAudioMixingMode(),
                        object : HMSActionResultListener {
                        override fun onError(error: HMSException) {
                            // error
                        }

                        override fun onSuccess() {
                            // success
                            dismiss()
                        }
                    })
                }
            }
        }

    private fun getAudioMixingMode(): AudioMixingMode {
        val selectedView =
            binding.root.findViewById<RadioButton>(binding.rgMusicModeSelector.checkedRadioButtonId)

        return when (selectedView.id) {
            R.id.btn_talk -> AudioMixingMode.TALK_ONLY
            R.id.btn_talk_music -> AudioMixingMode.TALK_AND_MUSIC
            R.id.btn_music -> AudioMixingMode.MUSIC_ONLY
            else -> AudioMixingMode.TALK_AND_MUSIC
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
           meetingViewModel.setAudioMixingMode(getAudioMixingMode())
            dismiss()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.startButton.setOnClickListener {
            val mediaProjectionManager: MediaProjectionManager = requireContext().getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager
            resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

        binding.stopButton.setOnClickListener {
            meetingViewModel.stopAudioshare(object : HMSActionResultListener{
                override fun onError(error: HMSException) {
                    // Error Event
                }

                override fun onSuccess() {
                    // Success event
                    dismiss()
                }
            })
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