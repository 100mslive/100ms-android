package live.hms.roomkit.ui.meeting.chat

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearSmoothScroller

import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.R
import live.hms.roomkit.ui.meeting.AllowedToMessageParticipants
import live.hms.roomkit.ui.meeting.ChatPauseState
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.video.sdk.models.HMSPeer


/**
 * Meant to listen to the adapter and set up the observerables that will
 * be needed across various classes.
 * Meant to work with the [SingleSideFadeRecyclerview]
 */
class ChatUseCase {
    private fun getOverallChatState(
        meetingViewModel : MeetingViewModel,
        chatViewModel: ChatViewModel,
        isChatEnabled: () -> Boolean,
        isUserBlocked : () -> Boolean,
        getCurrentRecipient: () -> Recipient?,
        chatPauseState: ChatPauseState,
        getAllowedRecipients : () -> AllowedToMessageParticipants?): ChatUiVisibilityState {
        return if(!isChatEnabled()) {
            ChatUiVisibilityState.DisabledFromLayout
        } else if(isUserBlocked()/*chatViewModel.isUserBlockedFromChat()*/) {
            ChatUiVisibilityState.Blocked
        } else {
            if(!chatPauseState.enabled)
                ChatUiVisibilityState.Paused(chatPauseState)
            else {
                val chatSendingEnabled = getAllowedRecipients()?.isChatSendingEnabled()
                // Role fix
                val tempRecipient = getCurrentRecipient()
                if( tempRecipient == null && chatSendingEnabled == true) {
                    // Take it out of null when it shouldn't be
                    val recToMessage = meetingViewModel.defaultRecipientToMessage()
                    chatViewModel.updateSelectedRecipientChatBottomSheet(recToMessage)
                }
                if( tempRecipient != null && chatSendingEnabled == false) {
                    // Make it null when it should be.
                    val recToMessage = meetingViewModel.defaultRecipientToMessage()
                    chatViewModel.updateSelectedRecipientChatBottomSheet(recToMessage)
                }
                // Might be no recipients, might need a recipient select.
                if(getCurrentRecipient() == null && meetingViewModel.defaultRecipientToMessage() == null) {
                    if(getAllowedRecipients()?.peers == true)
                        ChatUiVisibilityState.RecipientSelectNeeded
                    else
                        ChatUiVisibilityState.NoRecipients
                }
                else
                    ChatUiVisibilityState.Enabled
            }
        }
    }
    fun initiate(
        messages: MutableLiveData<List<ChatMessage>>,
        chatPauseState: MutableLiveData<ChatPauseState>,
        roleChanged : MutableLiveData<HMSPeer>, //used to refresh options
        viewlifecycleOwner: LifecycleOwner,
        chatAdapter: ChatAdapter,
        recyclerview: SingleSideFadeRecyclerview,
        chatViewModel: ChatViewModel,
        meetingViewModel: MeetingViewModel,
        emptyIndicator: View? = null,
        sendButton: ImageView,
        editText: EditText,
        bannedText: TextView,
        chatPausedBy: TextView,
        chatPausedContainer: LinearLayoutCompat,
        recipientPickerContainer: LinearLayout,
        isChatEnabled: () -> Boolean,
        getAllowedRecipients : () -> AllowedToMessageParticipants?,
        currentRbac : () -> Recipient?
//        canShowIndicator : () -> Boolean = {true}
    ) {

        recyclerview.adapter = chatAdapter
        toggleEmptyIndicator(emptyIndicator, messages.value)
        // Chat pause observer
        roleChanged.observe(viewlifecycleOwner) {
            val overallChatState =
                getOverallChatState(
                    meetingViewModel,
                    chatViewModel,
                    isChatEnabled,
                    chatViewModel::isUserBlockedFromChat,
                    currentRbac,
                    chatPauseState.value!!,
                    getAllowedRecipients

                )

                pauseBlockRegularUi(
                    sendButton,
                    editText,
                    bannedText,
                    chatPausedBy,
                    chatPausedContainer,
                    recipientPickerContainer,
                    overallChatState
                )
        }
        chatPauseState.observe(viewlifecycleOwner) { pauseState ->
            if(isChatEnabled()) {
                val overallChatState = getOverallChatState(
                    meetingViewModel,
                    chatViewModel,
                    isChatEnabled,
                    chatViewModel::isUserBlockedFromChat,
                    currentRbac,
                    pauseState,
                    getAllowedRecipients
                )

                if (isChatEnabled()) {
                    pauseBlockRegularUi(
                        sendButton,
                        editText,
                        bannedText,
                        chatPausedBy,
                        chatPausedContainer,
                        recipientPickerContainer,
                        overallChatState
                    )
                }
            }
        }
        // Chat messages observer
        messages.observe(viewlifecycleOwner) {
            if (isChatEnabled()) {
                val overallChatState = getOverallChatState(
                    meetingViewModel,
                    chatViewModel,
                    isChatEnabled,
                    chatViewModel::isUserBlockedFromChat,
                    currentRbac,
                    chatPauseState.value!!,
                    getAllowedRecipients
                )
                pauseBlockRegularUi(
                    sendButton,
                    editText,
                    bannedText,
                    chatPausedBy,
                    chatPausedContainer,
                    recipientPickerContainer,
                    overallChatState
                )
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

    sealed class ChatUiVisibilityState(
        val msgView : Boolean, // Not even needed because it's handled differently.
                                // Plus it's always visible.
        val msgEdit : Boolean,
        val msgRecPicker : Boolean // This is only different from msgEdit in recipient select needed
    ) {
        object DisabledFromLayout : ChatUiVisibilityState(false, false, false)
        data class Paused(val pausedState: ChatPauseState) : ChatUiVisibilityState(true, false, false)
        object Blocked : ChatUiVisibilityState(true, false, false)
        object NoRecipients : ChatUiVisibilityState(true, false, false)
        object RecipientSelectNeeded : ChatUiVisibilityState(true, false, true)
        object Enabled : ChatUiVisibilityState(true, true, true)
    }
    /**
     * What are chat states?
     * These are the chat elements:
     *  - messages recyclerview (msgView) (1, v)
     *  - message sending edit text (msgEdit) (2, e)
     *  - recipient picker (msgRecPicker) (3, p)
     *
     * 1. Totally disabled from the layout.  (v 0, e 0, p 0)
     * 2. Enabled from layout:
     *    - Chat closed (v:0, e:0, p:0)
     *    - Chat opened (v:1, e:1, p:1)
     *    - Chat Paused (v:1, e:0, p:0)
     *    - User Blocked (v:1, e:0, p:0)
     *    - No recipients (v:1, e:0, p:0)
     *    - Recipient select needed (v:1, e:0, p:1)
     */
    private fun pauseBlockRegularUi(sendButton: ImageView,
                                    editText: EditText,
                                    bannedText : TextView,
                                    chatPausedBy : TextView,
                                    chatPausedContainer : LinearLayoutCompat,
                                    recipientPickerContainer : LinearLayout,
                                    state : ChatUiVisibilityState) {

        sendButton.visibility = if(state.msgEdit) View.VISIBLE else View.GONE
        editText.visibility = if(state.msgEdit) View.VISIBLE else View.GONE
        recipientPickerContainer.visibility = if(state.msgRecPicker) View.VISIBLE else View.GONE

        when(state) {
            ChatUiVisibilityState.Blocked -> {
                bannedText.visibility = View.VISIBLE
                chatPausedContainer.visibility = View.GONE
            }
            ChatUiVisibilityState.DisabledFromLayout -> {} // Nothing to do  since the visibility toggle handled it
            ChatUiVisibilityState.NoRecipients -> {
                bannedText.visibility = View.GONE
                chatPausedContainer.visibility = View.GONE
            } // Nothing to do since the visibility toggle handled it
            is ChatUiVisibilityState.Paused -> {
                chatPausedBy.text = bannedText.context.getString(R.string.chat_paused_by, state.pausedState.updatedBy.userName)
                chatPausedContainer.visibility = View.VISIBLE
                bannedText.visibility = View.GONE
            }
            ChatUiVisibilityState.RecipientSelectNeeded -> {
                // Change the picker maybe?
            }
            ChatUiVisibilityState.Enabled -> {
                bannedText.visibility = View.GONE
                chatPausedContainer.visibility = View.GONE
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