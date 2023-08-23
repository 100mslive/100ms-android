package live.hms.roomkit.ui.meeting.chat

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

/**
 * Meant to listen to the adapter and set up the observerables that will
 * be needed across various classes.
 * Meant to work with the [SingleSideFadeRecyclerview]
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
                val position = it.size - 1
                if(position < 0)
                    return@postDelayed
                // Without this sometimes the view won't update.
                chatAdapter.notifyItemChanged(position ,null)
                // Scroll to the new message
                recyclerview.smoothScrollToPosition(position)
            }, 300)
        }
    }
}