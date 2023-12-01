package live.hms.roomkit.ui.meeting.chat.combined

import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.R
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.SessionMetadataUseCase
import live.hms.roomkit.ui.meeting.participants.PinnedMessageItem
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape
import live.hms.roomkit.util.dp

class PinnedMessageUiUseCase {
    private val pinnedMessagesAdapter = GroupieAdapter()
    lateinit var snap : PagerSnapHelper
    fun init(
        pinnedMessageRecyclerView: RecyclerView,
        pinCloseButton: ImageView,
        unpinMessage: (SessionMetadataUseCase.PinnedMessage) -> Unit,
        canPinMessages : Boolean
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
        snap = PagerSnapHelper().apply {
            attachToRecyclerView(pinnedMessageRecyclerView)
        }
        if(canPinMessages) {
            pinCloseButton.visibility = View.VISIBLE
            pinCloseButton.setOnSingleClickListener {
                val position = (pinnedMessageRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if(position != -1) {
                    val message = (pinnedMessagesAdapter.getItem(position) as PinnedMessageItem).receivedPinnedMessage
                    unpinMessage(message)
                }
            }
        } else {
            pinCloseButton.visibility = View.GONE
        }

    }

    fun messagesUpdate(pinnedMessages : Array<SessionMetadataUseCase.PinnedMessage>,
                       pinnedMessagesContainer : LinearLayoutCompat
                       ) {
        if(pinnedMessages.isEmpty()) {
            pinnedMessagesContainer.visibility = View.GONE
        } else {
            pinnedMessagesContainer.visibility = View.VISIBLE
        }
        pinnedMessagesContainer.background = getShape()// ResourcesCompat.getDrawable(this.root.resources,R.drawable.gray_shape_round_dialog, null)!!
            .apply {
                val color = getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.surfaceDefault,
                    HMSPrebuiltTheme.getDefaults().surface_default)
                colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
            }

        fun heightUpdated(size : Int, position : Int) {
            with(pinnedMessagesContainer.findViewById<RecyclerView>(R.id.pinnedMessagesRecyclerView)) {
                updateLayoutParams {
                    this.height = size + 16.dp()
//                    scrollToPosition(position)

                    post {
                        val view: View? = layoutManager?.findViewByPosition(position)
                        if(view != null) {
                            val snapDistance: IntArray =
                                snap.calculateDistanceToFinalSnap(layoutManager!!, view)!!
                            if (snapDistance[0] != 0 || snapDistance[1] != 0) {
                                scrollBy(snapDistance[0], snapDistance[1])
                            }
                        }
                    }
                }
                // scroll to the position
            }
//            pinnedMessagesContainer.height = size + 16.dp()
        }
        val group: Group = Section().apply {
            addAll(pinnedMessages.map { ExpandableGroup(PinnedMessageItem(it,::heightUpdated), false) })
        }
        pinnedMessagesAdapter.update(listOf(group))
    }

}