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
                  val onClick: () -> Unit = {},
  private val shouldShowMessageOptions : (ChatMessage) -> Boolean
  ) : ListAdapter<ChatMessage, ChatAdapter.ChatMessageViewHolder>(DIFFUTIL_CALLBACK) {
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
      binding.viewMore.setOnSingleClickListener(clickListener)
    }


    fun bind(sentMessage: ChatMessage) {
      with(binding) {
        name.text = sentMessage.senderName
        message.text = sentMessage.message
        blueBar.visibility = if (sentMessage.isSentByMe) View.VISIBLE else View.GONE
        if(sentMessage.time != null) {
          time.text = formatter.format(Date(sentMessage.time))
        }

        val isSentToVisible = sentMessage.toGroup != null
        sentTo.visibility = if(isSentToVisible)
           View.VISIBLE
        else
          View.GONE

        if(isSentToVisible) {
          val r = binding.root.resources
          sentTo.text =
            r.getString(R.string.chat_to_label,
              sentMessage.toGroup,
              if(sentMessage.isDmToMe || sentMessage.isDm)
                r.getString(R.string.chat_to_dm_label)
              else
                r.getString(R.string.chat_to_group_label)
            )
        }

        sentBackground.visibility = if(sentMessage.toGroup == null) {
          View.GONE
        } else
          View.VISIBLE


        viewMore.visibility = if(shouldShowMessageOptions(sentMessage)) View.VISIBLE else View.GONE
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
    val binding = ListItemChatBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return ChatMessageViewHolder(binding) { position -> openMessageOptions(getItem(position)) }
  }

  override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

}