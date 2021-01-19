package live.hms.android100ms.ui.home.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import live.hms.android100ms.databinding.FragmentSettingsBinding
import live.hms.android100ms.util.viewLifecycle
import org.appspot.apprtc.AppRTCAudioManager

class SettingsFragment : Fragment() {

  private var binding by viewLifecycle<FragmentSettingsBinding>()

  private lateinit var settings: SettingsStore
  private lateinit var commitHelper: SettingsStore.MultiCommitHelper

  private val audioDevices = ArrayList<HashMap<String, AppRTCAudioManager.AudioDevice>>()
  private val videoDevices = ArrayList<String>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentSettingsBinding.inflate(inflater, container, false)
    settings = SettingsStore(requireContext())
    commitHelper = settings.MultiCommitHelper()

    initButtons()
    initEditTexts()
    initAutoCompleteViews()
    initSwitches()
    return binding.root
  }

  private fun initButtons() {
    binding.containerAdvanced.apply {
      visibility = View.GONE // Default to hidden

      binding.buttonAdvancedSettings.setOnClickListener {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
      }
    }

  }

  private fun initNonEmptyEditText(
    defaultText: String,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    saveOnValid: (value: String) -> Unit
  ) {
    editText.apply {
      setText(defaultText)
      addTextChangedListener { text ->
        val value = text.toString()
        if (value.isEmpty()) {
          container.error = "$name cannot be empty"
        } else {
          container.error = null
          saveOnValid(value)
        }
      }
    }
  }

  private fun initNonEmptyEditTextWithRange(
    defaultText: Int,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    minValue: Int, maxValue: Int,
    saveOnValid: (value: Int) -> Unit
  ) {
    editText.apply {
      setText(defaultText.toString())
      addTextChangedListener { text ->
        if (text.toString().isEmpty()) {
          container.error = "$name cannot be empty"
        } else {
          val value = text.toString().toInt()
          if (value < minValue || value > maxValue) {
            container.error = "$name value should be in range [$minValue, $maxValue] inclusive"
          } else {
            container.error = null
            saveOnValid(value)
          }
        }
      }
    }
  }

  private fun initEditTexts() {
    binding.apply {
      initNonEmptyEditText(
        settings.username,
        editTextName, containerName,
        "Username"
      ) { commitHelper.setUsername(it) }

      initNonEmptyEditText(
        settings.environment,
        editTextEnvironment, containerEnvironment,
        "Environment"
      ) { commitHelper.setEnvironment(it) }


      initNonEmptyEditTextWithRange(
        settings.videoGridRows,
        editTextRows, containerRows,
        "Maximum Rows",
        1, 5,
      ) { commitHelper.setVideoGridRows(it) }

      initNonEmptyEditTextWithRange(
        settings.videoGridColumns,
        editTextColumns, containerColumns,
        "Maximum Rows",
        1, 5,
      ) { commitHelper.setVideoGridColumns(it) }

      initNonEmptyEditTextWithRange(
        settings.videoFrameRate,
        editTextVideoFramerate, containerVideoFramerate,
        "Video Frame Rate",
        1, 30,
      ) { commitHelper.setVideoFrameRate(it) }
    }
  }

  private fun initAutoCompleteViews() {
    // TODO
    binding.apply {
      autoCompleteVideoSource.isEnabled = false
      autoCompleteMicrophoneSource.isEnabled = false
      autoCompleteResolution.isEnabled = false
      autoCompleteCodecs.isEnabled = false
      autoCompleteVideoBitrate.isEnabled = false
    }
  }

  private fun initSwitch(
    defaultIsChecked: Boolean,
    view: SwitchMaterial,
    saveOnChange: (isChecked: Boolean) -> Unit
  ) {
    view.apply {
      isChecked = defaultIsChecked
      setOnCheckedChangeListener { _, isChecked -> saveOnChange(isChecked) }
    }
  }

  private fun initSwitches() {
    binding.apply {
      initSwitch(
        !settings.publishVideo,
        switchMuteVideoOnJoin
      ) { commitHelper.setPublishVideo(!it) }

      initSwitch(
        !settings.publishAudio,
        switchMuteMicrophoneOnJoin
      ) { commitHelper.setPublishAudio(!it) }

      // Disable the switches not yet supported (TODO)
      switchMirrorVideo.isEnabled = false
      switchShowPreviewBeforeJoin.isEnabled = false
    }
  }

  override fun onPause() {
    super.onPause()

    // We commit the changes only once. Such that
    // the SharedPreferenceChange listener is fired once
    commitHelper.commit()
  }
}