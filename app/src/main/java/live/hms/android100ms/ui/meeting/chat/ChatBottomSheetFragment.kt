package live.hms.android100ms.ui.meeting.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.android100ms.databinding.FragmentBottomSheetChatBinding
import live.hms.android100ms.model.RoomDetails
import live.hms.android100ms.util.viewLifecycle
import java.util.*
import kotlin.collections.ArrayList

class ChatBottomSheetFragment : BottomSheetDialogFragment() {

  companion object {
    private const val TAG = "ChatFragment"
  }

  private var binding by viewLifecycle<FragmentBottomSheetChatBinding>()
  private val args: ChatBottomSheetFragmentArgs by navArgs()

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
    binding = FragmentBottomSheetChatBinding.inflate(inflater, container, false)
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
    binding.editTextMessage.apply {
      addTextChangedListener { text ->
        binding.fabSendMessage.isEnabled = text.toString().isNotEmpty()
      }
    }

    binding.fabSendMessage.apply {
      isEnabled = false // Disabled by default
      setOnClickListener {
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
}