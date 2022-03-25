package live.hms.app2.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import live.hms.app2.BuildConfig
import live.hms.app2.databinding.FragmentSettingsBinding
import live.hms.app2.ui.meeting.MeetingViewMode
import live.hms.app2.util.ENV_PROD
import live.hms.app2.util.ENV_QA
import live.hms.app2.util.viewLifecycle
import live.hms.video.utils.HMSLogger
import java.util.*


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

    private val ENVIRONMENTS = arrayOf(
      ENV_PROD,
      ENV_QA,
    )

    private const val FRONT_FACING_CAMERA = "user"
    private const val REAR_FACING_CAMERA = "environment"

    private val CAMERAS = mapOf(
      "Front Facing Camera" to FRONT_FACING_CAMERA,
      "Rear Facing Camera" to REAR_FACING_CAMERA,
    )

    private val MEETING_MODES = MeetingViewMode::class.nestedClasses.mapNotNull { it.simpleName }.toTypedArray()

    private val LOG_LEVELS_100MS = HMSLogger.LogLevel.values().map { it.toString() }.toTypedArray()

  }

  private var binding by viewLifecycle<FragmentSettingsBinding>()
  private val args: SettingsFragmentArgs by navArgs()

  private lateinit var settings: SettingsStore
  private lateinit var commitHelper: SettingsStore.MultiCommitHelper
  private lateinit var mode: SettingsMode

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentSettingsBinding.inflate(inflater, container, false)
    settings = SettingsStore(requireContext())
    commitHelper = settings.MultiCommitHelper()
    mode = args.mode

    initEditTexts()
    initAutoCompleteViews()
    initSwitches()
    return binding.root
  }

  private fun handleVisibility(allowedMode: EnumSet<SettingsMode>, container: ViewGroup) {
    if (allowedMode.contains(mode)) {
      container.visibility = View.VISIBLE
    } else {
      container.visibility = View.GONE
    }
  }

  private fun initNonEmptyEditText(
    allowedMode: EnumSet<SettingsMode>,
    defaultText: String,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    saveOnValid: (value: String) -> Unit
  ) {
    handleVisibility(allowedMode, container)
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
    allowedMode: EnumSet<SettingsMode>,
    defaultText: Int,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    minValue: Int, maxValue: Int,
    saveOnValid: (value: Int) -> Unit
  ) {
    handleVisibility(allowedMode, container)
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
    allowedMode: EnumSet<SettingsMode>,
    defaultText: Long,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    minValue: Long, maxValue: Long,
    saveOnValid: (value: Long) -> Unit
  ) {
    handleVisibility(allowedMode, container)
    editText.apply {
      setText(defaultText.toString())
      addTextChangedListener { text ->
        if (text.toString().isEmpty()) {
          container.error = "$name cannot be empty"
        } else {
          val value = text.toString().toLong()
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
    allowedMode: EnumSet<SettingsMode>,
    defaultText: Float,
    editText: TextInputEditText,
    container: TextInputLayout,
    name: String,
    minValue: Float, maxValue: Float,
    saveOnValid: (value: Float) -> Unit
  ) {
    handleVisibility(allowedMode, container)
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
        EnumSet.of(SettingsMode.HOME),
        settings.username,
        editTextName, containerName,
        "Username"
      ) { commitHelper.setUsername(it) }

      // TODO: Make rows, columns available in SettingsMode.MEETING
      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME),
        settings.videoGridRows,
        editTextRows, containerRows,
        "Maximum Rows",
        1, 3,
      ) { commitHelper.setVideoGridRows(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME),
        settings.videoGridColumns,
        editTextColumns, containerColumns,
        "Maximum Rows",
        1, 3,
      ) { commitHelper.setVideoGridColumns(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME),
        settings.audioPollInterval,
        editTextAudioPollInterval, containerAudioPollInterval,
        "Audio Poll Interval",
        100, 10000
      ) { commitHelper.setAudioPollInterval(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME),
        settings.silenceAudioLevelThreshold,
        editTextSilenceAudioLevelThreshold,
        containerSilenceAudioLevelThreshold,
        "Silence Audio Level Threshold",
        0, 100
      ) { commitHelper.setSilenceAudioLevelThreshold(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
        settings.videoFrameRate,
        editTextVideoFramerate, containerVideoFramerate,
        "Video Frame Rate",
        1, 30,
      ) { commitHelper.setVideoFrameRate(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
        settings.videoResolutionWidth,
        editTextResolutionWidth, containerResolutionWidth,
        "Width",
        1, 3840
      ) { commitHelper.setVideoResolutionWidth(it) }

      initNonEmptyEditTextWithRange(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
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
        EnumSet.of(SettingsMode.HOME),
        containerVideoSource,
        autoCompleteVideoSource,
        CAMERAS.filterValues { it == settings.camera }.keys.first(),
        CAMERAS.keys.toTypedArray(),
      ) { commitHelper.setCamera(CAMERAS.getValue(it)) }

      initAutoCompleteView(
        EnumSet.of(SettingsMode.HOME),
        containerCodecs,
        autoCompleteCodecs,
        settings.codec,
        CODECS,
      ) { commitHelper.setCodec(it) }

      initAutoCompleteView(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
        containerVideoBitrate,
        autoCompleteVideoBitrate,
        VIDEO_BITRATE.filterValues { it == settings.videoBitrate }.keys.first(),
        VIDEO_BITRATE.keys.toTypedArray(),
      ) { commitHelper.setVideoBitrate(VIDEO_BITRATE.getValue(it)) }

      initAutoCompleteView(
        EnumSet.of(SettingsMode.HOME),
        containerMeetingMode,
        autoCompleteMeetingMode,
        settings.meetingMode.toString(),
        MEETING_MODES,
      ) { commitHelper.setMeetingMode(it) }

      initAutoCompleteView(
        EnumSet.of(SettingsMode.HOME),
        containerLogLevelsWebrtc,
        autoCompleteLogLevelsWebrtc,
        settings.logLevelWebrtc.toString(),
        LOG_LEVELS_100MS,
      ) { commitHelper.setLogLevelWebRtc(it) }

      initAutoCompleteView(
        EnumSet.of(SettingsMode.HOME),
        containerLogLevels100msSdk,
        autoCompleteLogLevels100msSdk,
        settings.logLevel100msSdk.toString(),
        LOG_LEVELS_100MS,
      ) { commitHelper.setLogLevel100msSdk(it) }

      if (BuildConfig.INTERNAL) {
        initEditableAutoCompleteView(
          EnumSet.of(SettingsMode.HOME),
          containerEnvironment,
          autoCompleteEnvironment,
          settings.environment,
          "Environment",
          ENVIRONMENTS,
        ) { commitHelper.setEnvironment(it) }
      } else {
        containerEnvironment.visibility = View.GONE
      }
    }
  }

  private fun initEditableAutoCompleteView(
    allowedMode: EnumSet<SettingsMode>,
    container: TextInputLayout,
    view: AutoCompleteTextView,
    defaultText: String,
    name: String,
    items: Array<String>,
    saveOnValid: (value: String) -> Unit
  ) {
    initAutoCompleteView(
      allowedMode,
      container,
      view,
      defaultText,
      items,
      saveOnValid,
    )

    view.addTextChangedListener { text ->
      val value = text.toString()
      if (value.isEmpty()) {
        container.error = "$name cannot be empty"
      } else {
        container.error = null
        saveOnValid(value)
      }
    }
  }


  private fun initAutoCompleteView(
    allowedMode: EnumSet<SettingsMode>,
    container: TextInputLayout,
    view: AutoCompleteTextView,
    defaultText: String,
    items: Array<String>,
    saveOnValid: (value: String) -> Unit
  ) {
    handleVisibility(allowedMode, container)
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
    allowedMode: EnumSet<SettingsMode>,
    defaultIsChecked: Boolean,
    view: SwitchMaterial,
    saveOnChange: (isChecked: Boolean) -> Unit
  ) {
    view.apply {
      visibility = if (allowedMode.contains(mode)) View.VISIBLE else View.GONE
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
        EnumSet.of(SettingsMode.HOME),
        settings.enableSubscribeDegradation,
        switchSubscribeDegradationEnabled
      ) { commitHelper.setSubscribeDegradation(it) }

      initSwitch(
        EnumSet.of(SettingsMode.HOME),
        settings.enableHardwareAEC,
        switchUseHardwareEchoCancellation
      ) { commitHelper.setUseHardwareAEC(it) }

      initSwitch(
        EnumSet.of(SettingsMode.HOME),
        settings.showStats,
        showStats
      ) { commitHelper.setShowStats(it) }

      initSwitch(
        EnumSet.of(SettingsMode.HOME),
        settings.publishVideo,
        switchPublishVideoOnJoin
      ) { commitHelper.setPublishVideo(it) }

      initSwitch(
        EnumSet.of(SettingsMode.HOME),
        settings.publishAudio,
        switchPublishAudioOnJoin
      ) { commitHelper.setPublishAudio(it) }

      initSwitch(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
        settings.showReconnectingProgressBars,
        switchShowProgressBars
      ) { commitHelper.setReconnectingShowProgressBars(it) }


      initSwitch(
        EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
        settings.showPreviewBeforeJoin,
        switchShowPreviewBeforeJoin
      ) { commitHelper.setShowPreviewBeforeJoin(it) }

      if (BuildConfig.INTERNAL) {
        initSwitch(
          EnumSet.of(SettingsMode.HOME, SettingsMode.MEETING),
          settings.isLeakCanaryEnabled,
          switchToggleLeakCanary
        ) { handleLeakCanaryToggle(it) }
      } else {
        switchToggleLeakCanary.isEnabled = false
        switchToggleLeakCanary.visibility = View.GONE
      }

      // TODO: Make detectDominantSpeaker, audioLevel settings available in SettingsMode.MEETING
      initSwitch(
        EnumSet.of(SettingsMode.HOME),
        settings.detectDominantSpeaker,
        switchShowDominantSpeaker
      ) { commitHelper.setDetectDominantSpeaker(it) }

      // Disable the switches not yet implemented
      switchShowNetworkInfo.isEnabled = false
      switchPublishAudioOnJoin.isEnabled = false
      switchPublishVideoOnJoin.isEnabled = false
      switchMirrorVideo.isEnabled = false

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