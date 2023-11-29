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
        val isTest = false
        // If the user can't block or pin message, hide the entire dialog.
        val allowedToBlock = isTest || meetingViewModel.isAllowedToBlockFromChat()
        val allowedToPin = isTest || meetingViewModel.isAllowedToPinMessages()
        val allowedToHideMessages = isTest || meetingViewModel.isAllowedToHideMessages()
        if(!( allowedToPin || allowedToBlock || allowedToHideMessages) )
            return

        MessageOptionsBottomSheet(chatMessage, allowedToBlock, allowedToPin, allowedToHideMessages).apply {
            arguments = bundleOf(CHAT_MESSAGE_OPTIONS_EXTRA to chatMessage)
        }.show(childFragmentManager, TAG)
    }
}