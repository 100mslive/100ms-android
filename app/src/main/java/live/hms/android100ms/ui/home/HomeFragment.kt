package live.hms.android100ms.ui.home

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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import live.hms.android100ms.R
import live.hms.android100ms.api.Status
import live.hms.android100ms.databinding.FragmentHomeBinding
import live.hms.android100ms.model.CreateRoomRequest
import live.hms.android100ms.model.RecordingInfo
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.ui.home.settings.SettingsStore
import live.hms.android100ms.ui.meeting.MeetingActivity
import live.hms.android100ms.util.ROOM_DETAILS
import live.hms.android100ms.util.viewLifecycle

class HomeFragment : Fragment() {

  companion object {
    private const val TAG = "HomeFragment"
  }

  private var binding by viewLifecycle<FragmentHomeBinding>()
  private lateinit var homeViewModel: HomeViewModel
  private lateinit var settings: SettingsStore

  override fun onResume() {
    super.onResume()
    val data = requireActivity().intent.data
    Log.v(TAG, "onResume(): Trying to update $data into EditTextMeetingUrl")

    if (data != null) {
      val url = data.toString()
      val urlIsValid = updateAndVerifyMeetingUrl(url)
      if (urlIsValid) {
        binding.editTextMeetingUrl.setText(url)
      }
    }

  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_settings -> {
        findNavController().navigate(
          HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
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

    initViewModel()
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


  private fun initViewModel() {
    homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
    observeLiveData()
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
    binding.buttonStartMeeting.visibility = View.GONE
    binding.buttonJoinMeeting.visibility = View.GONE

    binding.containerCardStartMeeting.visibility = View.GONE
    binding.containerCardJoin.visibility = View.GONE
    binding.containerCardName.visibility = View.GONE
    binding.progressBar.root.visibility = View.VISIBLE

    binding.containerRoomName.isEnabled = false
    binding.containerMeetingUrl.isEnabled = false
    binding.containerRoomName.isEnabled = false
    binding.switchRecord.isEnabled = false
  }

  private fun hideProgressBar() {
    binding.buttonStartMeeting.visibility = View.VISIBLE
    binding.buttonJoinMeeting.visibility = View.VISIBLE

    binding.containerCardStartMeeting.visibility = View.VISIBLE
    binding.containerCardJoin.visibility = View.VISIBLE
    binding.containerCardName.visibility = View.VISIBLE
    binding.progressBar.root.visibility = View.GONE

    binding.containerRoomName.isEnabled = true
    binding.containerMeetingUrl.isEnabled = true
    binding.containerRoomName.isEnabled = true
    binding.switchRecord.isEnabled = true
  }

  private fun getUsername() = binding.editTextName.text.toString()

  private fun tryJoiningRoomAs(role: String) {
    val username = getUsername()

    // Update the name in local store if required
    if (binding.checkboxUseDefault.isChecked) {
      settings.username = username
    }

    homeViewModel.sendAuthTokenRequest(
      TokenRequest(
        roomId = settings.lastUsedRoomId,
        username = username,
        role = role,
        environment = settings.environment
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
            putExtra(ROOM_DETAILS, roomDetails)
            startActivity(this)
            requireActivity().finish()
          }
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

    homeViewModel.createRoomResponse.observe(viewLifecycleOwner) { response ->
      when (response.status) {
        Status.LOADING -> {
          updateProgressBarUI(true)
          showProgressBar()
        }
        Status.SUCCESS -> {
          val data = response.data!!
          Toast.makeText(
            requireContext(),
            "Created room ${data.roomId} \uD83E\uDD73",
            Toast.LENGTH_SHORT
          ).show()
          tryJoiningRoomAs("Host")
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
      val roomId = uri.getQueryParameter("room")!!
      val host = uri.host!!
      val environment = host.split('.')[0]

      settings.lastUsedRoomId = roomId
      settings.environment = environment
    } catch (e: Exception) {
      Log.e(TAG, "Cannot update $url", e)
      allOk = false
      binding.containerMeetingUrl.error = "Meeting URL missing room query param"
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
        "https://${settings.environment}.100ms.live/?" +
            arrayOf(
              "room=${settings.lastUsedRoomId}",
              "env=${settings.environment}",
              "role=Guest"
            ).joinToString("&")
      }
      else -> ""
    }

    binding.editTextMeetingUrl.setText(url)

    mapOf(
      binding.editTextName to binding.containerName,
      binding.editTextMeetingUrl to binding.containerMeetingUrl,
      binding.editTextRoomName to binding.containerRoomName
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

      val meetingUrl = binding.editTextMeetingUrl.text.toString()
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

      if (allOk) tryJoiningRoomAs("Guest")
    }

    binding.buttonStartMeeting.setOnClickListener {
      var allOk = isValidUserName()
      val enableRecording = binding.switchRecord.isChecked

      val roomName = binding.editTextRoomName.text.toString()
      if (roomName.isEmpty()) {
        allOk = false
        binding.containerRoomName.error = "Room Name cannot be empty"
      }

      if (allOk) {
        homeViewModel.sendCreateRoomRequest(
          CreateRoomRequest(
            roomName,
            settings.environment,
            RecordingInfo(enableRecording)
          )
        )
      }
    }
  }
}