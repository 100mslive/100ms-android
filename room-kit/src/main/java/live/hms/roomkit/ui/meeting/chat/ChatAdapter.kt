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
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.theme.applyTheme
import java.text.SimpleDateFormat
import java.util.*


class ChatAdapter(private val openMessageOptions : (ChatMessage) -> Unit,
                  val onClick: () -> Unit = {}) : ListAdapter<ChatMessage, ChatAdapter.ChatMessageViewHolder>(DIFFUTIL_CALLBACK) {
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
      val clickListener = fun ( _ : View ) {
        // Setting a click listener on the entire root. Watch
        //  out if there are other buttons on this.
        openMessageOptions(bindingAdapterPosition)
        onClick()
      }
      binding.root.setOnSingleClickListener(clickListener)
      binding.name.setOnSingleClickListener(clickListener)
      binding.time.setOnSingleClickListener(clickListener)
      binding.message.setOnSingleClickListener(clickListener)
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

}