package live.hms.roomkit.ui.meeting.chat.combined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedTabChatBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.PauseChatUIUseCase
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.meeting.chat.rbac.RoleBasedChatBottomSheet
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle
import kotlin.reflect.KFunction0

class CombinedChatFragmentTab(val dismissAllowingStateLoss: KFunction0<Unit>) : Fragment() {
    private var binding by viewLifecycle<LayoutChatParticipantCombinedTabChatBinding>()
    val meetingViewModel : MeetingViewModel by activityViewModels()
    private val chatViewModel : ChatViewModel by activityViewModels()
    private val launchMessageOptionsDialog = LaunchMessageOptionsDialog()
    private val chatAdapter by lazy { ChatAdapter({ message ->
        launchMessageOptionsDialog.launch(meetingViewModel,
        childFragmentManager, message) },{}, { MessageOptionsBottomSheet.showMessageOptions(meetingViewModel)})
    }
    private val pinnedMessageUiUseCase = PinnedMessageUiUseCase()

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


    var roleChangeSingleShot = -1
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        roleChangeSingleShot = meetingViewModel.roleChangeSingleShot.value ?:0
        binding.sendToBackground.setOnSingleClickListener {
            RoleBasedChatBottomSheet.launch(childFragmentManager, chatViewModel)
        }
        meetingViewModel.roleChangeSingleShot.observe(viewLifecycleOwner) {
            if(roleChangeSingleShot != it) {
                roleChangeSingleShot = it
                dismissAllowingStateLoss()
            }
        }
        meetingViewModel.initPrebuiltChatMessageRecipient.observe(viewLifecycleOwner) {
            chatViewModel.setInitialRecipient(it.first, it.second)
        }
        chatViewModel.currentlySelectedRecipientRbac.observe(viewLifecycleOwner) { recipient ->
            ChatRbacRecipientHandling().updateChipRecipientUI(binding.sendToChipText, recipient)
        }
        meetingViewModel.messageIdsToHide.observe(viewLifecycleOwner) { messageIdsToHide ->
            chatViewModel.updateMessageHideList(messageIdsToHide)
        }
        meetingViewModel.currentBlockList.observe(viewLifecycleOwner) { chatBlockedPeerIdsList ->
            chatViewModel.updateBlockList(chatBlockedPeerIdsList)
        }
        pinnedMessageUiUseCase.init(binding.pinnedMessagesRecyclerView, binding.pinCloseButton, meetingViewModel::unPinMessage, meetingViewModel.isAllowedToPinMessages())
        PauseChatUIUseCase().setChatPauseVisible(binding.chatOptionsCard, meetingViewModel)
        ChatUseCase().initiate(
            chatViewModel.messages,
            meetingViewModel.chatPauseState,
            meetingViewModel.roleChange,
            meetingViewModel.currentBlockList,
            viewLifecycleOwner,
            chatAdapter,
            binding.chatMessages,
            chatViewModel,
            meetingViewModel,
            binding.emptyIndicator,
            binding.iconSend,
            binding.editTextMessage,
            binding.userBlocked,
            binding.chatPausedBy,
            binding.chatPausedContainer,
            binding.chatExtra,
            meetingViewModel.prebuiltInfoContainer::isChatEnabled,
            meetingViewModel::availableRecipientsForChat,
            chatViewModel::currentlySelectedRbacRecipient
        )
        meetingViewModel.broadcastsReceived.observe(viewLifecycleOwner) {
            chatViewModel.receivedMessage(it)
        }
        meetingViewModel.pinnedMessages.observe(viewLifecycleOwner) { pinnedMessages ->
            pinnedMessageUiUseCase.messagesUpdate(pinnedMessages,
                binding.pinnedMessagesDisplay)
        }

    }
}
