package live.hms.android100ms.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.android100ms.R
import live.hms.android100ms.databinding.FragmentChatBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.ui.meeting.MeetingFragmentArgs
import live.hms.android100ms.util.viewLifecycle
import java.util.*
import kotlin.collections.ArrayList

class ChatFragment : Fragment() {

    companion object {
        private const val TAG = "ChatFragment"
    }

    private var binding by viewLifecycle<FragmentChatBinding>()
    private val args: MeetingFragmentArgs by navArgs()

    private val chatViewModel: ChatViewModel by activityViewModels()
    private lateinit var roomDetails: RoomDetails

    private val messages = ArrayList<ChatMessage>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModels()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        roomDetails = args.roomDetail

        initRecyclerView()
        initTextFields()

        return binding.root
    }

    private fun initRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ChatAdapter(requireContext(), messages)
            scrollToPosition(messages.size - 1)
        }
    }

    private fun initViewModels() {
        chatViewModel.getMessages().observe(viewLifecycleOwner) {
            messages.clear()
            messages.addAll(it)
            binding.recyclerView.apply {
                adapter?.notifyDataSetChanged()
                scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun initTextFields() {
        binding.editTextMessage.addTextChangedListener { text ->
            binding.fabSendMessage.isEnabled = text.toString().isNotEmpty()
        }

        binding.fabSendMessage.setOnClickListener {
            val message = ChatMessage(
                roomDetails.username,
                Date(),
                binding.editTextMessage.text.toString(),
                true
            )
            chatViewModel.broadcast(message)
            binding.editTextMessage.setText("")
        }
    }
}