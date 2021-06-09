package live.hms.app2.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import live.hms.app2.R
import live.hms.app2.api.Status
import live.hms.app2.databinding.FragmentHomeBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.model.TokenRequest
import live.hms.app2.ui.meeting.MeetingActivity
import live.hms.app2.ui.settings.SettingsFragment
import live.hms.app2.ui.settings.SettingsMode
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.*
import java.util.*

class HomeFragment : Fragment() {

  companion object {
    private const val TAG = "HomeFragment"
  }

  private var binding by viewLifecycle<FragmentHomeBinding>()
  private val homeViewModel: HomeViewModel by viewModels()
  private lateinit var settings: SettingsStore

  private val tokenEndpoint: String
    get() = getTokenEndpoint(getTokenEnvironmentFromInitEnvironment(settings.environment))

  override fun onResume() {
    super.onResume()
    val data = requireActivity().intent.data
    Log.v(TAG, "onResume(): Trying to update $data into EditTextMeetingUrl")

    if (data != null && data.toString().isNotEmpty()) {
      updateAndVerifyMeetingUrl(data.toString())
      requireActivity().intent.data = null
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_settings -> {
        findNavController().navigate(
          HomeFragmentDirections.actionHomeFragmentToSettingsFragment(SettingsMode.HOME)
        )
      }
      R.id.action_email_logs -> {
        requireContext().startActivity(
          EmailUtils.getNonFatalLogIntent(requireContext())
        )
      }
    }
    return false
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentHomeBinding.inflate(inflater, container, false)
    settings = SettingsStore(requireContext())

    setHasOptionsMenu(true)

    observeLiveData()
    initEditTextViews()
    initConnectButton()
    initOnBackPress()
    hideProgressBar()

    return binding.root
  }

  private fun initOnBackPress() {
    requireActivity().apply {
      onBackPressedDispatcher.addCallback(
        this@HomeFragment.viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            Log.v(TAG, "initOnBackPress -> handleOnBackPressed")
            finish()
          }
        })
    }
  }


  @SuppressLint("SetTextI18n")
  private fun updateProgressBarUI(isRoomCreator: Boolean) {
    val headingPrefix = if (isRoomCreator) "Creating room for" else "Joining as"
    binding.progressBar.heading.text = "$headingPrefix ${getUsername()}..."

    val descriptionDefaults = if (settings.publishVideo && settings.publishAudio) {
      "Video and microphone will be turned on by default.\n"
    } else if (settings.publishVideo && !settings.publishVideo) {
      "Only audio will be turned on by default\n"
    } else if (!settings.publishVideo && settings.publishVideo) {
      "Only video will be turned on by default\n"
    } else {
      "Video and microphone will be turned off by default.\n"
    }

    val descriptionSetting = "You can change the defaults in the app settings."
    binding.progressBar.description.text = descriptionDefaults + descriptionSetting
  }

  private fun showProgressBar() {
    binding.buttonJoinMeeting.visibility = View.GONE

    binding.containerCardJoin.visibility = View.GONE
    binding.containerCardName.visibility = View.GONE
    binding.progressBar.root.visibility = View.VISIBLE

    binding.containerMeetingUrl.isEnabled = false
  }

  private fun hideProgressBar() {
    binding.buttonJoinMeeting.visibility = View.VISIBLE

    binding.containerCardJoin.visibility = View.VISIBLE
    binding.containerCardName.visibility = View.VISIBLE
    binding.progressBar.root.visibility = View.GONE

    binding.containerMeetingUrl.isEnabled = true
  }

  private fun getUsername() = binding.editTextName.text.toString()

  private fun tryJoiningRoomAs(role: String) {
    val username = getUsername()

    // Update the name in local store
    settings.username = username

    homeViewModel.sendAuthTokenRequest(
      tokenEndpoint,
      TokenRequest(
        roomId = settings.lastUsedRoomId,
        userId = UUID.randomUUID().toString() + username.replace(
          " ",
          "-"
        ), // Can be any customer facing userId
        role = role.trim().toLowerCase(Locale.ENGLISH),
      )
    )
  }

  private fun observeLiveData() {
    homeViewModel.authTokenResponse.observe(viewLifecycleOwner) { response ->
      when (response.status) {
        Status.LOADING -> {
          updateProgressBarUI(false)
          showProgressBar()
        }
        Status.SUCCESS -> {
          // No need to hide progress bar here, as we directly move to
          // the next page

          val data = response.data!!
          val roomDetails = RoomDetails(
            env = settings.environment,
            roomId = settings.lastUsedRoomId,
            username = getUsername(),
            authToken = data.token
          )
          Log.v(TAG, "Auth Token: ${roomDetails.authToken}")

          // Start the meeting activity
          Intent(requireContext(), MeetingActivity::class.java).apply {
            LogUtils.staticFileWriterStart(requireContext(), roomDetails.roomId)
            putExtra(ROOM_DETAILS, roomDetails)
            startActivity(this)
          }
          requireActivity().finish()
        }
        Status.ERROR -> {
          hideProgressBar()
          Toast.makeText(
            requireContext(),
            response.message,
            Toast.LENGTH_SHORT
          ).show()
        }
      }
    }

  }

  private fun updateAndVerifyMeetingUrl(url: String): Boolean {
    var allOk = true
    try {
      val uri = Uri.parse(url)
      val room = Regex("/[a-zA-Z0-9]+/([a-zA-Z0-9]+)/?.*").find(uri.path ?: "")
      val roomId = room!!.groups[1]!!.value

      settings.lastUsedRoomId = roomId

      uri.host?.let { host ->
        if (host.contains("prod2.100ms.live")) {
          settings.environment = SettingsFragment.ENV_PROD
        } else if (host.contains("qa2.100ms.live")) {
          settings.environment = SettingsFragment.ENV_QA
        }
      }

      binding.editTextMeetingUrl.setText(roomId)
    } catch (e: Exception) {
      Log.e(TAG, "Cannot update $url", e)
      allOk = false
      binding.containerMeetingUrl.error = "Meeting url do not have roomId and env"
    }

    return allOk
  }

  private fun initEditTextViews() {
    // Load the data if saved earlier (easy debugging)
    binding.editTextName.setText(settings.username)

    // TODO: Set text as a meeting URL obtained by parsing the TOKEN_ENDPOINT
    // TODO: Will the meeting link always point to 100ms.live base url?


    val data = requireActivity().intent.data

    val url = when {
      data != null -> {
        Log.v(TAG, "Received Meeting URI via Intent: $data")
        data.toString()
      }

      settings.lastUsedRoomId.isNotEmpty() -> {
        "https://qa2.100ms.live/meeting/${settings.lastUsedRoomId}"
      }
      else -> ""
    }

    url.isNotBlank() && updateAndVerifyMeetingUrl(url)

    mapOf(
      binding.editTextName to binding.containerName,
      binding.editTextMeetingUrl to binding.containerMeetingUrl
    ).forEach {
      it.key.addTextChangedListener { text ->
        if (text.toString().isNotEmpty()) it.value.error = null
      }
    }
  }

  private fun isValidUserName(): Boolean {
    val username = binding.editTextName.text.toString()
    if (username.isEmpty()) {
      binding.containerName.error = "Username cannot be empty"
      return false
    }
    return true
  }

  private fun initConnectButton() {
    binding.buttonJoinMeeting.setOnClickListener {
      var allOk = isValidUserName()

      val meetingUrl = binding.editTextMeetingUrl.text.toString().trim()
      val validUrl = URLUtil.isValidUrl(meetingUrl)
      if (meetingUrl.isEmpty()) {
        allOk = false
        binding.containerMeetingUrl.error = "Meeting URL cannot be empty"
      } else if (meetingUrl.contains(" ") && !validUrl) {
        allOk = false
        binding.containerMeetingUrl.error = "Meeting URL or Meeting ID is invalid"
      } else if (validUrl) {
        // Save both environment and room-id
        allOk = allOk && updateAndVerifyMeetingUrl(meetingUrl)
      } else {
        // No spaces, and not a url -- could only be a room-id
        settings.lastUsedRoomId = meetingUrl
      }

      if (allOk) tryJoiningRoomAs(SettingsStore(requireContext()).role)
    }
  }
}