package live.hms.roomkit.ui.meeting.chat.combined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedTabChatBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.CHAT_MESSAGE_OPTIONS_EXTRA
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.meeting.participants.PinnedMessageItem
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle

class CombinedChatFragmentTab : Fragment() {
    private var binding by viewLifecycle<LayoutChatParticipantCombinedTabChatBinding>()
    val meetingViewModel : MeetingViewModel by activityViewModels()
    private val chatViewModel : ChatViewModel by activityViewModels()
    private val launchMessageOptionsDialog = LaunchMessageOptionsDialog()
    private val chatAdapter by lazy { ChatAdapter({ message ->
        launchMessageOptionsDialog.launch(meetingViewModel,
        childFragmentManager, message) })
    }
    val pinnedMessageUiUseCase = PinnedMessageUiUseCase()

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
        pinnedMessageUiUseCase.init(binding.pinnedMessagesRecyclerView)
        ChatUseCase().initiate(chatViewModel.messages, viewLifecycleOwner, chatAdapter, binding.chatMessages, chatViewModel, binding.emptyIndicator,
            binding.iconSend, binding.editTextMessage, binding.userBlocked) {
            meetingViewModel.prebuiltInfoContainer.isChatEnabled()
        }
        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }
        meetingViewModel.pinnedMessages.observe(viewLifecycleOwner) { pinnedMessages ->
            pinnedMessageUiUseCase.messagesUpdate(pinnedMessages,
                binding.pinnedMessagesDisplay)
        }

    }
}
