package live.hms.app2.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import live.hms.app2.ui.meeting.chat.ChatViewModel
import live.hms.video.sdk.HMSSDK

class ChatViewModelFactory(
    private val hmsSdk: HMSSDK
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(hmsSdk) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}