package live.hms.android100ms.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import live.hms.android100ms.R
import live.hms.android100ms.api.Status
import live.hms.android100ms.databinding.FragmentHomeBinding
import live.hms.android100ms.model.CreateRoomRequest
import live.hms.android100ms.model.RecordingInfo
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.ui.meeting.MeetingActivity
import live.hms.android100ms.util.ROOM_DETAILS
import live.hms.android100ms.util.SettingsStore
import live.hms.android100ms.util.viewLifecycle

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var binding by viewLifecycle<FragmentHomeBinding>()
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var settings: SettingsStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        settings = SettingsStore(requireContext())
        initViewModel()
        initSwitches()
        initEditTextViews()
        initConnectButton()
        return binding.root
    }

    private fun initViewModel() {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        observeLiveData()
    }

    private fun initSwitches() {
        binding.switchIsJoin.setOnCheckedChangeListener { _, isJoin ->
            val buttonText = if (isJoin) R.string.join_room else R.string.create_room
            val roomEditTextText = if (isJoin) R.string.room_id else R.string.room_name

            binding.containerRoom.hint = resources.getString(roomEditTextText)
            binding.buttonRoom.setText(buttonText)
            binding.editTextRoom.text = null
            binding.switchRecord.apply {
                if (!isJoin) isChecked = false

                isEnabled = !isJoin
                visibility = if (isJoin) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showProgressBar() {
        binding.buttonRoom.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        binding.switchIsJoin.apply {
            visibility = View.GONE
            isEnabled = false
        }

        binding.containerRoom.isEnabled = false
        binding.containerUsername.isEnabled = false
        binding.containerEnv.isEnabled = false
    }

    private fun hideProgressBar() {
        binding.buttonRoom.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE

        binding.switchRecord.apply {
            // If in join room mode, disable switch record toggle
            isEnabled = !binding.switchIsJoin.isEnabled
        }

        binding.switchIsJoin.apply {
            visibility = View.GONE
            isEnabled = true
        }

        binding.containerRoom.isEnabled = true
        binding.containerUsername.isEnabled = true
        binding.containerEnv.isEnabled = true
    }

    private fun observeLiveData() {
        homeViewModel.authTokenResponse.observe(viewLifecycleOwner) { response ->
            when (response.status) {
                Status.LOADING -> {
                    showProgressBar()
                }
                Status.SUCCESS -> {
                    hideProgressBar()
                    val data = response.data!!
                    val roomDetails = RoomDetails(
                        env = binding.editTextEnv.text.toString(),
                        roomId = binding.editTextRoom.text.toString(),
                        username = binding.editTextUsername.text.toString(),
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
                    showProgressBar()
                }
                Status.SUCCESS -> {
                    val data = response.data!!
                    Toast.makeText(
                        requireContext(),
                        "Created room ${data.roomId} \uD83E\uDD73",
                        Toast.LENGTH_SHORT
                    ).show()

                    homeViewModel.sendAuthTokenRequest(
                        TokenRequest(
                            roomId = data.roomId,
                            username = binding.editTextUsername.text.toString(),
                            role = "Host",
                            environment = binding.editTextEnv.text.toString()
                        )
                    )
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

    private fun initEditTextViews() {
        // Load the data if saved earlier (easy debugging)
        binding.editTextUsername.setText(settings.username)
        binding.editTextEnv.setText(settings.lastUsedEnv)
        binding.editTextRoom.setText(settings.lastUsedRoomId)

        val data = requireActivity().intent.data
        if (data != null) {
            Log.v(TAG, "Received Meeting URI via Intent: $data")
            val roomId = data.getQueryParameter("room")
            val host = data.host!!
            val environment = host.split('.')[0]
            Log.v(TAG, "Incoming: room-id:$roomId, host:$host")

            binding.editTextEnv.setText(environment)
            binding.editTextRoom.setText(roomId)
            binding.switchIsJoin.isChecked = true
        }

        mapOf(
            binding.editTextEnv to binding.containerEnv,
            binding.editTextRoom to binding.containerRoom,
            binding.editTextUsername to binding.containerUsername
        ).forEach {
            it.key.addTextChangedListener { text ->
                if (text.toString().isNotEmpty()) it.value.error = null
            }
        }
    }

    private fun initConnectButton() {
        binding.buttonRoom.setOnClickListener {
            var allOk = true
            val isJoin = binding.switchIsJoin.isChecked
            val enableRecording = binding.switchRecord.isChecked

            val env = binding.editTextEnv.text.toString()
            if (env.isEmpty()) {
                allOk = false
                binding.containerEnv.error = "Env cannot be empty"
            }

            val room = binding.editTextRoom.text.toString()
            if (room.isEmpty()) {
                allOk = false
                binding.containerRoom.error = "Room Name cannot be empty"
            }

            val username = binding.editTextUsername.text.toString()
            if (username.isEmpty()) {
                allOk = false
                binding.containerUsername.error = "Username cannot be empty"
            }

            if (allOk) {
                // Save this username, env
                settings.username = username
                settings.lastUsedEnv = env

                if (isJoin) {
                    homeViewModel.sendAuthTokenRequest(TokenRequest(room, username, "Guest", env))
                    settings.lastUsedRoomId = room
                } else {
                    homeViewModel.sendCreateRoomRequest(
                        CreateRoomRequest(
                            room,
                            env,
                            RecordingInfo(enableRecording)
                        )
                    )
                }
            }
        }
    }
}