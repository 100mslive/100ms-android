package live.hms.android100ms.ui.home.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentSettingsBinding
import live.hms.android100ms.util.viewLifecycle
import org.appspot.apprtc.AppRTCAudioManager

class SettingsFragment : Fragment() {

  private var binding by viewLifecycle<FragmentSettingsBinding>()

  private val settings = SettingsStore(requireContext())

  private val audioDevices = ArrayList<HashMap<String, AppRTCAudioManager.AudioDevice>>()
  private val videoDevices = ArrayList<String>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentSettingsBinding.inflate(inflater, container, false)
    initDevices()
    initButtons()
    return binding.root
  }

  private fun initDevices() {
    val manager = AppRTCAudioManager.create(requireContext())
    val devices = manager.audioDevices

  }


  private fun initButtons() {
    binding.containerAdvanced.apply {
      visibility = View.GONE // Default to hidden

      binding.buttonAdvancedSettings.setOnClickListener {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
      }
    }

  }

  private fun commitChanges() {
    // TODO: Add Video Source, Audio Source

    binding.apply {
      settings.MultiCommitHelper()
        .setUsername(editTextName.text.toString())
        .setVideoGridRows(editTextRows.text.toString().toInt())
        .setVideoGridColumns(editTextColumns.text.toString().toInt())
        .setPublishVideo(!switchMuteVideoOnJoin.isChecked)
        .setPublishAudio(!switchMuteMicrophoneOnJoin.isChecked)
        .setVideoResolution(autoCompleteResolution.text.toString())
        .setCodec(autoCompleteCodecs.text.toString())
        .setVideoBitrate(editTextVideoBitrate.text.toString().toInt())
        .setVideoFrameRate(editTextVideoFramerate.text.toString().toInt())
        .commit()

    }
  }

  override fun onPause() {
    super.onPause()

    // We commit the changes only once. Such that
    // the SharedPreferenceChange listener is fired once
    commitChanges()
  }
}