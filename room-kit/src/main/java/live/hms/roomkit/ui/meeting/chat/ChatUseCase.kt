package live.hms.roomkit.ui.meeting.chat

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearSmoothScroller

import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch


/**
 * Meant to listen to the adapter and set up the observerables that will
 * be needed across various classes.
 * Meant to work with the [SingleSideFadeRecyclerview]
 */
class ChatUseCase {
    fun initiate(
        messages: MutableLiveData<List<ChatMessage>>,
        viewlifecycleOwner: LifecycleOwner,
        chatAdapter: ChatAdapter,
        recyclerview: SingleSideFadeRecyclerview,
        chatViewModel: ChatViewModel,
        emptyIndicator: View? = null,
        sendButton: ImageView,
        editText: EditText,
        bannedText : TextView,
        isChatEnabled: () -> Boolean,
//        canShowIndicator : () -> Boolean = {true}
    ) {

        recyclerview.adapter = chatAdapter
        toggleEmptyIndicator(emptyIndicator, messages.value)
        messages.observe(viewlifecycleOwner) {
            if (isChatEnabled()) {
            if(chatViewModel.isUserBlockedFromChat()) {
                // Then their edit text etc is hidden.
                sendButton.visibility = View.GONE
                editText.visibility = View.GONE
                bannedText.visibility = View.VISIBLE
            }
            toggleEmptyIndicator(emptyIndicator, it)
            val chatList = mutableListOf<ChatMessage>()
            chatList.addAll(it)
            chatAdapter.submitList(chatList)
            val position = it.size - 1
            if (position >= 0) {
                // Scroll to the new message
                val smoothScroller: RecyclerView.SmoothScroller =
                    object : LinearSmoothScroller(recyclerview.context) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }
                    }
                smoothScroller.targetPosition = position
                recyclerview.layoutManager!!.startSmoothScroll(smoothScroller)
                if (recyclerview.visibility == View.VISIBLE)
                    chatViewModel.unreadMessagesCount.postValue(0)
            }
        }
        }
    }

    private fun toggleEmptyIndicator(
        emptyIndicator: View?,
        messages: List<ChatMessage>?,
    ) {
        emptyIndicator?.visibility = if( messages.isNullOrEmpty() ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}