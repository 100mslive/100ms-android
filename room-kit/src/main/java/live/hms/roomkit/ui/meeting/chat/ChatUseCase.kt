package live.hms.roomkit.ui.meeting.chat

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearSmoothScroller

import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.ChatState


/**
 * Meant to listen to the adapter and set up the observerables that will
 * be needed across various classes.
 * Meant to work with the [SingleSideFadeRecyclerview]
 */
class ChatUseCase {
    fun initiate(
        messages: MutableLiveData<List<ChatMessage>>,
        chatPauseState : MutableLiveData<ChatState>,
        viewlifecycleOwner: LifecycleOwner,
        chatAdapter: ChatAdapter,
        recyclerview: SingleSideFadeRecyclerview,
        chatViewModel: ChatViewModel,
        emptyIndicator: View? = null,
        sendButton: ImageView,
        editText: EditText,
        bannedText : TextView,
        chatPausedBy : TextView,
        chatPausedContainer : LinearLayoutCompat,
        isChatEnabled: () -> Boolean,
        getChatState: () -> ChatState,
//        canShowIndicator : () -> Boolean = {true}
    ) {

        recyclerview.adapter = chatAdapter
        toggleEmptyIndicator(emptyIndicator, messages.value)
        // Chat pause observer
        chatPauseState.observe(viewlifecycleOwner) { state ->
            if (isChatEnabled()) {
                pauseBlockRegularUi(
                    chatViewModel,
                    sendButton,
                    editText,
                    bannedText,
                    chatPausedBy,
                    chatPausedContainer,
                    state
                )
            }
        }
        // Chat messages observer
        messages.observe(viewlifecycleOwner) {
            if (isChatEnabled()) {
                val state = getChatState()
            pauseBlockRegularUi(chatViewModel,
                sendButton,
                editText,
                bannedText,
                chatPausedBy,
                chatPausedContainer,
                state)
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

    private fun pauseBlockRegularUi(chatViewModel: ChatViewModel,
                                    sendButton: ImageView,
                                    editText: EditText,
                                    bannedText : TextView,
                                    chatPausedBy : TextView,
                                    chatPausedContainer : LinearLayoutCompat,
                                    state : ChatState) {
        if(chatViewModel.isUserBlockedFromChat()) {
            // Then their edit text etc is hidden.
            sendButton.visibility = View.GONE
            editText.visibility = View.GONE
            bannedText.text = bannedText.context.getText(R.string.blocked_from_sending_messages)
            bannedText.visibility = View.VISIBLE
            chatPausedContainer.visibility = View.GONE
        } else if(!state.enabled) {
            sendButton.visibility = View.GONE
            editText.visibility = View.GONE
            chatPausedBy.text = bannedText.context.getString(R.string.chat_paused_by, state.updatedBy)
            chatPausedContainer.visibility = View.VISIBLE
            bannedText.visibility = View.GONE
        } else {
            // This isn't a given, depends on if the user decided to hide or show...
            sendButton.visibility = View.VISIBLE
            editText.visibility = View.VISIBLE
            bannedText.visibility = View.GONE
            chatPausedContainer.visibility = View.GONE
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