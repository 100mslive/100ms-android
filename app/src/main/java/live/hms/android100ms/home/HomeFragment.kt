package live.hms.android100ms.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentHomeBinding
import live.hms.android100ms.util.viewLifecycle

class HomeFragment : Fragment() {
    val TAG = "HomeFragment"

    private var binding by viewLifecycle<FragmentHomeBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        initEditTextViews()
        initConnectButton()
        return binding.root
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
        binding.buttonJoin.setOnClickListener {
            var allOk = false

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
                // TODO: Send network request
            }
        }
    }
}