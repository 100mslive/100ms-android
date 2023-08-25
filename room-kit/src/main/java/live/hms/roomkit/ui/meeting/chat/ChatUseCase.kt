package live.hms.roomkit.ui.meeting.chat

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearSmoothScroller

import androidx.recyclerview.widget.RecyclerView




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
            val position = it.size - 1
            if(position >= 0) {
                // Without this sometimes the view won't update.
                chatAdapter.notifyItemInserted(position)
                // Scroll to the new message
                val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(recyclerview.context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_END
                    }
                }
                smoothScroller.targetPosition = position
                recyclerview.layoutManager!!.startSmoothScroll(smoothScroller)
            }
        }
    }
}