package live.hms.android100ms.ui.meeting.chat

data class ChatMessageCollection(
  val peerId: String,
  val senderName: String,
  val isSentByMe: Boolean,
  val messages: MutableList<ChatMessage>,
) {
  companion object {

    @JvmStatic
    fun fromChatMessage(message: ChatMessage): ChatMessageCollection {
      return ChatMessageCollection(
        message.peerId,
        message.senderName,
        message.isSentByMe,
        arrayListOf(message)
      )
    }
  }
}
