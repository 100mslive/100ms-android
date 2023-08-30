package live.hms.roomkit.ui.meeting.chat.combined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedTabChatBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.ChatViewModelFactory
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.chat.ChatAdapter
import live.hms.roomkit.ui.meeting.chat.ChatUseCase
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.util.viewLifecycle

class CombinedChatFragmentTab : Fragment() {
    private var binding by viewLifecycle<LayoutChatParticipantCombinedTabChatBinding>()
    val meetingViewModel : MeetingViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by activityViewModels()
    val chatAdapter = ChatAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ChatUseCase().initiate(chatViewModel.messages, viewLifecycleOwner, chatAdapter, binding.chatMessages, chatViewModel)
    }

}
