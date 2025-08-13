package live.hms.roomkit.ui.meeting.chat

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSMessageResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.enums.HMSMessageType
import live.hms.video.sdk.models.role.HMSRole
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat

@ExperimentalCoroutinesApi
class ChatViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    
    private lateinit var hmsSdk: HMSSDK
    private lateinit var viewModel: ChatViewModel
    private lateinit var localPeer: HMSLocalPeer
    private lateinit var mockRole: HMSRole
    
    @Before
    fun setup() {
        hmsSdk = mock()
        localPeer = mock()
        mockRole = mock()
        
        whenever(localPeer.peerID).thenReturn("local-peer-id")
        whenever(localPeer.name).thenReturn("Test User")
        whenever(localPeer.customerUserID).thenReturn("customer-123")
        whenever(localPeer.hmsRole).thenReturn(mockRole)
        whenever(mockRole.name).thenReturn("host")
        whenever(hmsSdk.getLocalPeer()).thenReturn(localPeer)
        
        viewModel = ChatViewModel(hmsSdk)
    }

    @Test
    fun `test initial recipient is null`() {
        assertThat(viewModel.currentlySelectedRecipientRbac.value, nullValue())
    }

    @Test
    fun `test setInitialRecipient updates recipient when num is greater`() {
        val recipient = Recipient.Everyone
        viewModel.setInitialRecipient(recipient, 1)
        
        assertThat(viewModel.currentlySelectedRecipientRbac.value, equalTo(recipient))
    }

    @Test
    fun `test setInitialRecipient does not update when num is smaller`() {
        val recipient1 = Recipient.Everyone
        val recipient2 = mock<Recipient.Peer>()
        
        viewModel.setInitialRecipient(recipient1, 2)
        viewModel.setInitialRecipient(recipient2, 1)
        
        assertThat(viewModel.currentlySelectedRecipientRbac.value, equalTo(recipient1))
    }

    @Test
    fun `test updateSelectedRecipientChatBottomSheet updates recipient`() {
        val recipient = Recipient.Everyone
        viewModel.updateSelectedRecipientChatBottomSheet(recipient)
        
        assertThat(viewModel.currentlySelectedRecipientRbac.value, equalTo(recipient))
    }

    @Test
    fun `test sendMessage to Everyone calls broadcast`() {
        val messageText = "Hello everyone!"
        viewModel.updateSelectedRecipientChatBottomSheet(Recipient.Everyone)
        
        viewModel.sendMessage(messageText)
        
        verify(hmsSdk).sendBroadcastMessage(
            eq(messageText),
            eq(HMSMessageType.CHAT),
            any()
        )
    }

    @Test
    fun `test sendMessage to specific peer calls directMessage`() {
        val messageText = "Hello peer!"
        val targetPeer: HMSPeer = mock()
        whenever(targetPeer.peerID).thenReturn("target-peer-id")
        whenever(targetPeer.name).thenReturn("Target User")
        
        viewModel.updateSelectedRecipientChatBottomSheet(Recipient.Peer(targetPeer))
        
        viewModel.sendMessage(messageText)
        
        verify(hmsSdk).sendDirectMessage(
            eq(messageText),
            eq(HMSMessageType.CHAT),
            eq(targetPeer),
            any()
        )
    }

    @Test
    fun `test sendMessage to role calls groupMessage`() {
        val messageText = "Hello role!"
        val targetRole: HMSRole = mock()
        whenever(targetRole.name).thenReturn("viewer")
        
        viewModel.updateSelectedRecipientChatBottomSheet(Recipient.Role(targetRole))
        
        viewModel.sendMessage(messageText)
        
        verify(hmsSdk).sendGroupMessage(
            eq(messageText),
            eq(HMSMessageType.CHAT),
            eq(listOf(targetRole)),
            any()
        )
    }

    @Test
    fun `test sendMessage with null recipient does nothing`() {
        val messageText = "Hello!"
        viewModel.updateSelectedRecipientChatBottomSheet(null)
        
        viewModel.sendMessage(messageText)
        
        verifyNoInteractions(hmsSdk)
    }

    @Test
    fun `test broadcast message success adds message to list`() = testDispatcher.runBlockingTest {
        val messageText = "Broadcast message"
        val mockHmsMessage: HMSMessage = mock()
        
        viewModel.updateSelectedRecipientChatBottomSheet(Recipient.Everyone)
        
        // Capture the listener
        val listenerCaptor = argumentCaptor<HMSMessageResultListener>()
        
        viewModel.sendMessage(messageText)
        
        verify(hmsSdk).sendBroadcastMessage(
            eq(messageText),
            eq(HMSMessageType.CHAT),
            listenerCaptor.capture()
        )
        
        // Simulate success callback
        listenerCaptor.firstValue.onSuccess(mockHmsMessage)
        
        // Messages should be updated
        assertThat(viewModel.messages.value?.isNotEmpty(), equalTo(true))
    }

    @Test
    fun `test broadcast message error logs error`() {
        val messageText = "Broadcast message"
        val error = HMSException(1, "Test error", "Test", "Test", "Test")
        
        viewModel.updateSelectedRecipientChatBottomSheet(Recipient.Everyone)
        
        val listenerCaptor = argumentCaptor<HMSMessageResultListener>()
        
        viewModel.sendMessage(messageText)
        
        verify(hmsSdk).sendBroadcastMessage(
            eq(messageText),
            eq(HMSMessageType.CHAT),
            listenerCaptor.capture()
        )
        
        // Simulate error callback
        listenerCaptor.firstValue.onError(error)
        
        // Message should not be added on error
        assertThat(viewModel.messages.value?.isEmpty() ?: true, equalTo(true))
    }

    @Test
    fun `test markAllMessagesRead resets unread count`() {
        // Observer to track changes
        val observer: Observer<Int> = mock()
        viewModel.unreadMessagesCount.observeForever(observer)
        
        viewModel.markAllMessagesRead()
        
        verify(observer).onChanged(0)
    }

    @Test
    fun `test clearMessages clears all messages and resets count`() {
        val messagesObserver: Observer<List<ChatMessage>> = mock()
        val countObserver: Observer<Int> = mock()
        
        viewModel.messages.observeForever(messagesObserver)
        viewModel.unreadMessagesCount.observeForever(countObserver)
        
        viewModel.clearMessages()
        
        verify(messagesObserver).onChanged(emptyList())
        verify(countObserver).onChanged(0)
    }
}