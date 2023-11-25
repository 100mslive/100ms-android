package live.hms.roomkit.ui.meeting.chat.combined

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import live.hms.roomkit.ui.meeting.CHAT_MESSAGE_OPTIONS_EXTRA
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MessageOptionsBottomSheet
import live.hms.roomkit.ui.meeting.chat.ChatMessage

class LaunchMessageOptionsDialog {
    private val TAG = "LaunchMessageOptionsDialog"
    fun launch(meetingViewModel : MeetingViewModel,
               childFragmentManager : FragmentManager,
               chatMessage: ChatMessage,
               ) {
        // TODO RE-ENABLE
        // If the user can't block or pin message, hide the entire dialog.
        val allowedToBlock = true//meetingViewModel.isAllowedToBlockFromChat()
        val allowedToPin = true//meetingViewModel.isAllowedToPinMessages()
        if(!( allowedToPin || allowedToBlock) )
            return

        MessageOptionsBottomSheet(chatMessage, allowedToBlock, allowedToPin).apply {
            arguments = bundleOf(CHAT_MESSAGE_OPTIONS_EXTRA to chatMessage)
        }.show(childFragmentManager, TAG)
    }
}