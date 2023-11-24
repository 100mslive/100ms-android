package live.hms.roomkit.ui.meeting.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ListItemChatBinding
import live.hms.roomkit.ui.theme.applyTheme
import java.text.SimpleDateFormat
import java.util.*


class ChatAdapter(private val openMessageOptions : (ChatMessage) -> Unit) : ListAdapter<ChatMessage, ChatAdapter.ChatMessageViewHolder>(DIFFUTIL_CALLBACK) {
  private val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
  companion object {
    private val DIFFUTIL_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
      override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
        oldItem == newItem


      override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
        oldItem == newItem
    }
  }

  inner class ChatMessageViewHolder(val binding: ListItemChatBinding, openMessageOptions : (Int) -> Unit) :
    RecyclerView.ViewHolder(binding.root) {
    init {
        binding.applyTheme()
      binding.root.setOnClickListener {
        // Setting a click listener on the entire root. Watch
        //  out if there are other buttons on this.
        openMessageOptions(bindingAdapterPosition)
      }
    }


    fun bind(sentMessage: ChatMessage) {
      with(binding) {
        name.text = sentMessage.senderName
        message.text = sentMessage.message
        blueBar.visibility = if (sentMessage.isSentByMe) View.VISIBLE else View.GONE
        if(sentMessage.time != null) {
          time.text = formatter.format(Date(sentMessage.time))
        }
        sentTo.visibility = if(sentMessage.sentTo == null)
           View.GONE
        else
          View.VISIBLE
        toGroup.visibility = if(sentMessage.toGroup == null)
          View.GONE
        else
          View.VISIBLE
        sentBackground.visibility = if(sentMessage.toGroup == null && sentMessage.sentTo == null) {
          View.GONE
        } else
          View.VISIBLE
        sentTo.text = sentMessage.sentTo
        toGroup.text = sentMessage.toGroup
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
    val binding = ListItemChatBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return ChatMessageViewHolder(binding, { position -> openMessageOptions(getItem(position)) })
  }

  override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
    holder.bind(getItem(position))
  }
  private var blockedPeerIds : Set<String>? = setOf<String>()
  fun sendChatMessage(list: MutableList<ChatMessage>?) {
    // whenever submitlist is called, filter it.
    // peers can't be unblocked anyway but if they are, their previous messages
    //  remain filtered out
    val blockList = blockedPeerIds
    val newList = if(blockList == null) list
    else
      list?.filter { !blockList.contains(it.senderPeerId) }
    submitList(newList)
  }
  fun updateBlockList(chatBlockedPeerIdsList: List<String>?) {
    // What does the adapter have to do?
    // Basically turn on a filter.
    // Ok so part of the problem is that we call submit list directly :(
    blockedPeerIds = chatBlockedPeerIdsList?.toSet()
    // Refresh the current list
    sendChatMessage(currentList)
  }
}