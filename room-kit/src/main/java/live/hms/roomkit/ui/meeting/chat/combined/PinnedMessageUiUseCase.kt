package live.hms.roomkit.ui.meeting.chat.combined

import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.SessionMetadataUseCase
import live.hms.roomkit.ui.meeting.participants.PinnedMessageItem
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault

class PinnedMessageUiUseCase {
    private val pinnedMessagesAdapter = GroupieAdapter()
    fun init(
        pinnedMessageRecyclerView: RecyclerView,
        pinCloseButton: ImageView,
        unpinMessage: (SessionMetadataUseCase.PinnedMessage) -> Unit
    ) {
        pinnedMessageRecyclerView.adapter = pinnedMessagesAdapter
        pinnedMessageRecyclerView.layoutManager = LinearLayoutManager(pinnedMessageRecyclerView.context)
        pinnedMessageRecyclerView.addItemDecoration(LinePagerIndicatorDecoration(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            ),
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )

        ))
        PagerSnapHelper().attachToRecyclerView(pinnedMessageRecyclerView)
        pinCloseButton.setOnSingleClickListener {
            val position = (pinnedMessageRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if(position != -1) {
                val message = (pinnedMessagesAdapter.getItem(position) as PinnedMessageItem).receivedPinnedMessage
                unpinMessage(message)
            }
        }
    }

    fun messagesUpdate(pinnedMessages : Array<SessionMetadataUseCase.PinnedMessage>,
                       pinnedMessagesContainer : ConstraintLayout
                       ) {
        if(pinnedMessages.isEmpty()) {
            pinnedMessagesContainer.visibility = View.GONE
        } else {
            pinnedMessagesContainer.visibility = View.VISIBLE
        }
        val group: Group = Section().apply {
            addAll(pinnedMessages.map { PinnedMessageItem(it) })
        }
        pinnedMessagesAdapter.update(listOf(group))
    }

}