package live.hms.roomkit.ui.meeting.participants

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPinnedMessageBinding
import live.hms.roomkit.ui.meeting.SessionMetadataUseCase
import live.hms.roomkit.ui.theme.applyTheme

class PinnedMessageItem(private val receivedPinnedMessage: SessionMetadataUseCase.PinnedMessage)
    : BindableItem<LayoutPinnedMessageBinding>(receivedPinnedMessage.hashCode().toLong()) {
    override fun bind(viewBinding: LayoutPinnedMessageBinding, position: Int) {
        with(viewBinding) {
            applyTheme()
            pinnedMessage.text = receivedPinnedMessage.text
        }
    }

    override fun getLayout(): Int = R.layout.layout_pinned_message

    override fun initializeViewBinding(view: View): LayoutPinnedMessageBinding =
        LayoutPinnedMessageBinding.bind(view)
}