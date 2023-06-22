package live.hms.roomkit.ui.meeting.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.DialogBottomSheetChatBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.util.viewLifecycle

class ChatBottomSheetFragment : BottomSheetDialogFragment(), AdapterView.OnItemSelectedListener {

    companion object {
        private const val TAG = "ChatFragment"
        var isChatHintHidden = false
    }

    private var binding by viewLifecycle<DialogBottomSheetChatBinding>()

    private val chatViewModel: ChatViewModel by activityViewModels()
    private val meetingViewModel: MeetingViewModel by activityViewModels()

    private val args: ChatBottomSheetFragmentArgs by navArgs()
    private lateinit var currentUserCustomerId: String

    private val messages = ArrayList<ChatMessage>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModels()
        initSpinnerUpdates()
        initViews()
        initPinnedMessageUpdate()
    }

    private fun initPinnedMessageUpdate() {
        meetingViewModel.sessionMetadata.observe(viewLifecycleOwner) { pinned ->
            binding.pinnedMessage.apply {
                if (pinned == null) {
                    hintView.visibility = View.GONE
                } else {
                    hintMessageTextview.text = pinned
                    hintView.visibility = View.VISIBLE
                }
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBottomSheetChatBinding.inflate(inflater, container, false)
        currentUserCustomerId = args.currentUserCustomerId

        initToolbar()
        initRecyclerView()
        initButtons()
        // Once we open the chat, assume that all messages will be seen
        chatViewModel.unreadMessagesCount.postValue(0)

        return binding.root
    }

    private fun initSpinnerUpdates() {
        chatViewModel.chatMembers.observe(viewLifecycleOwner) {
            refreshSpinner(it.recipients, it.index)
        }
    }

    private fun initViews(){
        if (isChatHintHidden.not()) {
            binding.hintView.hintView.visibility = View.VISIBLE
        }
    }

    private fun refreshSpinner(recipientList: List<Recipient>, selectedIndex: Int) {
        val participantSpinner = binding.recipientSpinner
        ArrayAdapter(requireContext(), R.layout.layout_chat_recipient_selector_item, recipientList)
            .also { recipientsAdapter ->
                participantSpinner.adapter = recipientsAdapter
                participantSpinner.setSelection(selectedIndex, false)
                recipientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                participantSpinner.post { participantSpinner.onItemSelectedListener = this }
            }
    }

    private fun initRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ChatAdapter(messages, meetingViewModel::setSessionMetadata)
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
        binding.iconSend.setOnClickListener {
            val messageStr = binding.editTextMessage.text.toString().trim()
            if (messageStr.isNotEmpty()) {
                chatViewModel.sendMessage(messageStr)
                binding.editTextMessage.setText("")
            }
        }


        binding.hintView.btnCloseHint.setOnClickListener {
            binding.hintView.hintView.visibility = View.GONE
            isChatHintHidden = true
        }

        // Starts hidden by default
        binding.pinnedMessage.apply {
            hintView.visibility = View.GONE
            btnCloseHint.setOnClickListener {
                meetingViewModel.setSessionMetadata(null)
            }
            icon.setImageResource(R.drawable.ic_pinned_message)
        }
    }

    private fun initToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_close -> {
                    dismiss()
                }
            }
            true
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        chatViewModel.recipientSelected(parent?.getItemAtPosition(position) as Recipient)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Ignore it.
    }
}