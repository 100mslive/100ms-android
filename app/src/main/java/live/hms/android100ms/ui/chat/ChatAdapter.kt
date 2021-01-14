package live.hms.android100ms.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import live.hms.android100ms.databinding.ListItemChatBinding

class ChatAdapter(
    private val messages: ArrayList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

    inner class ChatMessageViewHolder(val binding: ListItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.name.text = message.senderName
            binding.message.text = message.message
            binding.timestamp.text = message.time.toString()
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