package live.hms.roomkit.ui.meeting.chat

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

/**
 * Meant to listen to the adapter and set up the observerables that will
 * be needed across various classes.
 */
class ChatUseCase {
    fun initiate(messages: MutableLiveData<ArrayList<ChatMessage>>,
                 viewlifecycleOwner: LifecycleOwner,
                 chatAdapter: ChatAdapter,
                 recyclerview: SingleSideFadeRecyclerview) {
        recyclerview.adapter = chatAdapter
        messages.observe(viewlifecycleOwner) {
            chatAdapter.submitList(it)
            recyclerview.postDelayed({
                // Without this sometimes the view won't update.
                chatAdapter.notifyItemChanged(it.size - 1 ,null)
                // Scroll to the new message
                recyclerview.smoothScrollToPosition(it.size -1)
            }, 300)
        }
    }
}