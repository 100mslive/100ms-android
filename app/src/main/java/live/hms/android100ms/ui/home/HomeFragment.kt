package live.hms.android100ms.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import live.hms.android100ms.api.Status
import live.hms.android100ms.databinding.FragmentHomeBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.model.TokenRequest
import live.hms.android100ms.util.viewLifecycle

class HomeFragment : Fragment() {
    val TAG = "HomeFragment"

    private var binding by viewLifecycle<FragmentHomeBinding>()
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        initViewModel()
        initEditTextViews()
        initConnectButton()
        return binding.root
    }

    private fun initViewModel() {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        observeLiveData()
    }

    private fun showProgressBar() {
        binding.buttonJoinRoom.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.buttonJoinRoom.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
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
                    val endpoint = binding.editTextEndpoint.text.toString();
                    val roomDetails = RoomDetails(
                        endpoint = endpoint,
                        roomId = binding.editTextRoomName.text.toString(),
                        username = binding.editTextUsername.text.toString(),
                        authToken = data.token
                    )
                    Log.v(TAG, "Auth Token: ${roomDetails.authToken}")
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToMeetingFragment(roomDetails)
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
        val data = requireActivity().intent.data
        if (data != null) {
            Log.v(TAG, "Received Meeting URI via Intent: $data")
            val roomId = data.getQueryParameter("room")
            val host = data.host
            Log.v(TAG, "Incoming: room-id:$roomId, host:$host")

            binding.editTextEndpoint.setText("wss://${host}/ws")
            binding.editTextRoomName.setText(roomId)
        }

        mapOf(
            binding.editTextEndpoint to binding.containerEndpoint,
            binding.editTextRoomName to binding.containerRoomName,
            binding.editTextUsername to binding.containerUsername
        ).forEach {
            it.key.addTextChangedListener { text ->
                if (text.toString().isNotEmpty()) it.value.error = null
            }
        }
    }

    private fun initConnectButton() {
        binding.buttonJoinRoom.setOnClickListener {
            var allOk = true

            val endpoint = binding.editTextEndpoint.text.toString()
            if (endpoint.isEmpty()) {
                allOk = false
                binding.containerEndpoint.error = "Endpoint cannot be empty"
            }

            val roomName = binding.editTextRoomName.text.toString()
            if (roomName.isEmpty()) {
                allOk = false
                binding.containerRoomName.error = "Room Name cannot be empty"
            }

            val username = binding.editTextUsername.text.toString()
            if (username.isEmpty()) {
                allOk = false
                binding.containerUsername.error = "Username cannot be empty"
            }

            if (allOk) {
                val env = endpoint.split(".")[0].replace("wss://", "")
                homeViewModel.sendAuthTokenRequest(TokenRequest(roomName, username, "Guest", env))
            }
        }
    }
}