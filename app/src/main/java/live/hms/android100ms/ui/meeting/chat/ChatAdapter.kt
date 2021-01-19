package live.hms.android100ms.ui.meeting.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import live.hms.android100ms.R
import live.hms.android100ms.databinding.ListItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
  private val context: Context,
  private val messages: ArrayList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

  private val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  inner class ChatMessageViewHolder(val binding: ListItemChatBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: ChatMessage) {
      binding.name.text = message.senderName
      binding.message.text = message.message
      binding.timestamp.text = simpleDateFormat.format(message.time)

      val messageTextColor = if (message.isSentByMe)
        R.color.secondaryTextColor
      else
        android.R.color.black

      val cardBackgroundColor = if (message.isSentByMe)
        R.color.secondaryColor
      else
        android.R.color.white

      binding.message.setTextColor(ContextCompat.getColor(context, messageTextColor))
      binding.cardMessage.setCardBackgroundColor(
        ContextCompat.getColor(
          context,
          cardBackgroundColor
        )
      )
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
    val binding = ListItemChatBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return ChatMessageViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
    holder.bind(messages[position])
  }

  override fun getItemCount() = messages.size
}