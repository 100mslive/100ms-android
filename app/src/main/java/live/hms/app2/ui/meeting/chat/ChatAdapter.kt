package live.hms.app2.ui.meeting.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemChatBinding
import java.util.*

class ChatAdapter(
  private val messages: ArrayList<String>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

  inner class ChatMessageViewHolder(val binding: ListItemChatBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: String) {
      binding.message.text = message
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