package live.hms.app2.ui.meeting.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemChatBinding
import live.hms.video.sdk.models.enums.HMSMessageRecipientType
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
  private val messages: ArrayList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

  private val dateFormatter = SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault())

  inner class ChatMessageViewHolder(val binding: ListItemChatBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(message: ChatMessage) {
      binding.name.text = "${message.senderName}${getRecipientText(message)}"
      binding.message.text = message.message
      binding.blueBar.visibility = if (message.isSentByMe) View.VISIBLE else View.GONE
      binding.time.text = dateFormatter.format(message.time)
    }

    private fun getRecipientText(message: ChatMessage): String =
      when(message.recipient.recipientType) {
        HMSMessageRecipientType.BROADCAST -> ""
        HMSMessageRecipientType.PEER -> " (to ${message.recipient.recipientPeer?.name})"
        HMSMessageRecipientType.ROLES -> " (to ${message.recipient.recipientRoles.firstOrNull()?.name})"
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