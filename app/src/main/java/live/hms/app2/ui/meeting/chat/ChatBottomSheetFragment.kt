package live.hms.app2.ui.meeting.chat

import android.os.Bundle
import android.view.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.DialogBottomSheetChatBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.util.viewLifecycle
import java.util.*
import kotlin.collections.ArrayList

class ChatBottomSheetFragment : BottomSheetDialogFragment() {

  companion object {
    private const val TAG = "ChatFragment"
  }

  private var binding by viewLifecycle<DialogBottomSheetChatBinding>()

  private val chatViewModel: ChatViewModel by activityViewModels()

  private val args: ChatBottomSheetFragmentArgs by navArgs()
  private lateinit var roomDetails: RoomDetails
  private lateinit var currentUserCustomerId: String

  private val messages = ArrayList<String>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViewModels()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DialogBottomSheetChatBinding.inflate(inflater, container, false)
    roomDetails = args.roomDetail
    currentUserCustomerId = args.currentUserCustomerId

    initToolbar()
    initRecyclerView()
    initButtons()

    // Once we open the chat, assume that all messages will be seen
    chatViewModel.unreadMessagesCount.postValue(0)

    return binding.root
  }

  private fun initRecyclerView() {
    binding.recyclerView.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = ChatAdapter(messages)
      scrollToPosition(messages.size - 1)
    }
  }

  private fun initViewModels() {
    chatViewModel.messages.observe(viewLifecycleOwner) {
      messages.clear()
      messages.addAll(it)
      binding.recyclerView.apply {
        adapter?.notifyDataSetChanged()
        scrollToPosition(messages.size - 1)
      }
    }
  }

  private fun initButtons() {
    binding.containerMessage.setEndIconOnClickListener {
      val messageStr = binding.editTextMessage.text.toString().trim()
      if (messageStr.isNotEmpty()) {
        val message = ChatMessage(
            currentUserCustomerId,
            roomDetails.username,
            Date(),
            messageStr,
            true
        )
        // chatViewModel.broadcast(message)
        binding.editTextMessage.setText("")
      }
    }
  }

  private fun initToolbar() {
    binding.toolbar.setOnMenuItemClickListener { item ->
      when(item.itemId) {
        R.id.action_close_chat -> { dismiss() }
      }
      true
    }
  }
}