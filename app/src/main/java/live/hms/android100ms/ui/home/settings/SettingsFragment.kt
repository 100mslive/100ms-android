package live.hms.android100ms.ui.home.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import live.hms.android100ms.BuildConfig
import live.hms.android100ms.databinding.FragmentSettingsBinding
import live.hms.android100ms.util.viewLifecycle


class SettingsFragment : Fragment() {

  companion object {
    private const val TAG = "SettingsFragment"

    private val VIDEO_RESOLUTIONS = mapOf(
      "4K (2160p)" to "3840 x 2160",
      "Full HD (1080p)" to "1920 x 1080",
      "HD (720p)" to "1280 x 720",
      "VGA (480p)" to "640 x 480",
      "QVGA (240p)" to "320 x 240",
      "QQVGA (120p)" to "160 x 120",
      "90p" to "90 x 90"
    )

    private val CODECS = arrayOf("VP8", "H264")

    private val VIDEO_BITRATE = mapOf(
      "Lowest (100 kbps)" to 100,
      "Low (256 kbps)" to 256,
      "Medium (512 kbps)" to 512,
      "High (1 mbps)" to 1024,
      "LAN (4 mbps)" to 4096
    )

    private const val FRONT_FACING_CAMERA = "user"
    private const val REAR_FACING_CAMERA = "environment"

    private val CAMERAS = mapOf(
      "Front Facing Camera" to FRONT_FACING_CAMERA,
      "Rear Facing Camera" to REAR_FACING_CAMERA,
    )
  }

  private var binding by viewLifecycle<FragmentSettingsBinding>()

  private lateinit var settings: SettingsStore
  private lateinit var commitHelper: SettingsStore.MultiCommitHelper

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

  private fun initNonEmptyEditTextWithRange(
    defaultText: Float,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    minValue: Float, maxValue: Float,
    saveOnValid: (value: Float) -> Unit
  ) {
    editText.apply {
      setText(defaultText.toString())
      addTextChangedListener { text ->
        if (text.toString().isEmpty()) {
          container.error = "$name cannot be empty"
        } else {
          val value = text.toString().toFloat()
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
        1, 3,
      ) { commitHelper.setVideoGridRows(it) }

      initNonEmptyEditTextWithRange(
        settings.videoGridColumns,
        editTextColumns, containerColumns,
        "Maximum Rows",
        1, 3,
      ) { commitHelper.setVideoGridColumns(it) }

      initNonEmptyEditTextWithRange(
        settings.silenceAudioLevelThreshold,
        editTextSilenceAudioLevelThreshold,
        containerSilenceAudioLevelThreshold,
        "Silence Audio Level Threshold",
        0.0f, 5.0f
      ) { commitHelper.setSilenceAudioLevelThreshold(it) }

      initNonEmptyEditTextWithRange(
        settings.videoFrameRate,
        editTextVideoFramerate, containerVideoFramerate,
        "Video Frame Rate",
        1, 30,
      ) { commitHelper.setVideoFrameRate(it) }

      initNonEmptyEditTextWithRange(
        settings.videoResolutionWidth,
        editTextResolutionWidth, containerResolutionWidth,
        "Width",
        1, 3840
      ) { commitHelper.setVideoResolutionWidth(it) }

      initNonEmptyEditTextWithRange(
        settings.videoResolutionHeight,
        editTextResolutionHeight, containerResolutionHeight,
        "Height",
        1, 2160
      ) { commitHelper.setVideoResolutionHeight(it) }
    }
  }

  private fun initAutoCompleteViews() {
    binding.apply {
      initAutoCompleteView(
        autoCompleteVideoSource,
        CAMERAS.filterValues { it == settings.camera }.keys.first(),
        CAMERAS.keys.toTypedArray(),
      ) { commitHelper.setCamera(CAMERAS.getValue(it)) }

      initAutoCompleteView(
        autoCompleteCodecs,
        settings.codec,
        CODECS,
      ) { commitHelper.setCodec(it) }

      initAutoCompleteView(
        autoCompleteVideoBitrate,
        VIDEO_BITRATE.filterValues { it == settings.videoBitrate }.keys.first(),
        VIDEO_BITRATE.keys.toTypedArray(),
      ) { commitHelper.setVideoBitrate(VIDEO_BITRATE.getValue(it)) }

    }
  }

  private fun initAutoCompleteView(
    view: AutoCompleteTextView,
    defaultText: String,
    items: Array<String>,
    saveOnValid: (value: String) -> Unit
  ) {
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
    view.apply {
      setText(defaultText)
      setAdapter(adapter)

      setOnItemClickListener { _, _, position, _ ->
        saveOnValid(items[position])
      }
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

  private var forceDismissLeakCanaryDialog = false

  private fun handleLeakCanaryToggle(isChecked: Boolean) {
    if (forceDismissLeakCanaryDialog) {
      /*
      Since we set a OnCheckedChangeListener on leak-canary switch,
      when we programmatically toggle it on negative-button click,
      it calls this function again -- Initiating a un-ending loop
      of dialogs.

      This flag aims to hack-fix this issue.
       */
      forceDismissLeakCanaryDialog = false
      return
    }

    AlertDialog.Builder(requireContext())
      .setMessage(
        "You're about to toggle leak canary which requires restarting this app. " +
            "The app will be closed when you press Confirm."
      )
      .setTitle("Confirm Change")
      .setPositiveButton("Confirm") { _, _ ->

        // Commit all the changes as we forcefully kill the app.
        commitHelper.setIsLeakCanaryEnabled(isChecked)
        commitHelper.commit()


        System.exit(0)
      }
      .setNegativeButton("Discard") { dialog, _ ->
        forceDismissLeakCanaryDialog = true

        // Revert back the changes
        binding.switchToggleLeakCanary.isChecked = !isChecked
        dialog.dismiss()
      }
      .setCancelable(false)
      .create()
      .show()

  }

  private fun initSwitches() {
    binding.apply {
      initSwitch(
        settings.publishVideo,
        switchPublishVideoOnJoin
      ) { commitHelper.setPublishVideo(it) }

      initSwitch(
        settings.publishAudio,
        switchPublishAudioOnJoin
      ) { commitHelper.setPublishAudio(it) }

      initSwitch(
        settings.isLeakCanaryEnabled,
        switchToggleLeakCanary
      ) { handleLeakCanaryToggle(it) }

      // Disable the switches not yet supported (TODO: Add support)
      switchMirrorVideo.isEnabled = false
      switchShowPreviewBeforeJoin.isEnabled = false

      // Disable leak-canary switch for non-debug builds
      switchToggleLeakCanary.isEnabled = BuildConfig.DEBUG
    }
  }

  override fun onPause() {
    super.onPause()

    // We commit the changes only once. Such that
    // the SharedPreferenceChange listener is fired once
    commitHelper.commit()
  }
}