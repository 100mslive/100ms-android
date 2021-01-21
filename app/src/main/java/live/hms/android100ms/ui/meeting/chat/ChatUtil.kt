package live.hms.android100ms.ui.meeting.chat

fun addMessageToChatCollections(
  collection: MutableList<ChatMessageCollection>,
  message: ChatMessage
) {
  if (collection.isNotEmpty()) {
    val lastMessage = collection.last()
    if (lastMessage.peerId == message.customerId) {
      lastMessage.messages.add(message)
      return
    }
  }

  // Add the message as new collection as previous message belongs
  // to another user
  collection.add(ChatMessageCollection.fromChatMessage(message))
}

