package live.hms.android100ms.ui.meeting.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import live.hms.android100ms.databinding.ListItemChatBinding
import java.util.*

class ChatAdapter(
  private val messages: ArrayList<ChatMessageCollection>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

  inner class ChatMessageViewHolder(val binding: ListItemChatBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: ChatMessageCollection) {
      binding.name.text = message.senderName
      val messages = message.messages.map { it.message }
      binding.message.text = messages.joinToString("\n")
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