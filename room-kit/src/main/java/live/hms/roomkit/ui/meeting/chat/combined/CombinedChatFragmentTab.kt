package live.hms.roomkit.ui.meeting.chat.combined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xwray.groupie.GroupieAdapter
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedTabChatBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.CHAT_MESSAGE_OPTIONS_EXTRA
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle

class CombinedChatFragmentTab : Fragment() {
    val TAG = "CombinedChatFragmentTab"
    private var binding by viewLifecycle<LayoutChatParticipantCombinedTabChatBinding>()
    val meetingViewModel : MeetingViewModel by activityViewModels()
    private val chatViewModel : ChatViewModel by activityViewModels()
    private val chatAdapter by lazy { ChatAdapter(::openMessageOptions) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutChatParticipantCombinedTabChatBinding.inflate(inflater, container, false)
        with(binding) {
            iconSend.setOnSingleClickListener {
                val messageStr = binding.editTextMessage.text.toString().trim()
                if (messageStr.isNotEmpty()) {
                    chatViewModel.sendMessage(messageStr)
                    binding.editTextMessage.setText("")
                }
            }
        }

        binding.applyTheme()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ChatUseCase().initiate(chatViewModel.messages, viewLifecycleOwner, chatAdapter, binding.chatMessages, chatViewModel, binding.emptyIndicator,
            binding.iconSend, binding.editTextMessage, binding.userBlocked) {
            meetingViewModel.prebuiltInfoContainer.isChatEnabled()
        }
        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }
        meetingViewModel.pinnedMessages.observe(viewLifecycleOwner) { pinnedMessages ->
            if(pinnedMessages.isNullOrEmpty()) {
                binding.pinnedMessagesDisplay.visibility = View.GONE
            } else {
                binding.pinnedMessagesDisplay.visibility = View.VISIBLE
            }

        }

    }

    private fun openMessageOptions(chatMessage: ChatMessage,) {
        // If the user can't block or pin message, hide the entire dialog.
        if(!(meetingViewModel.isAllowedToBlockFromChat() || meetingViewModel.isAllowedToPinMessages()) )
            return

        MessageOptionsBottomSheet(chatMessage).apply {
            arguments = bundleOf(CHAT_MESSAGE_OPTIONS_EXTRA to chatMessage)
        }.show(childFragmentManager, TAG)
    }
}
