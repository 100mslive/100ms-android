package live.hms.roomkit.ui.meeting.participants

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
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
            pinnedMessage.text = boldTheSenderName(receivedPinnedMessage.text)
        }
    }

    private fun boldTheSenderName(text : String) : SpannableString {
        if (!TextUtils.isEmpty(text)) {
            val index = text.indexOf(':')

            if (index > 0) {
                val spanBuilder = SpannableStringBuilder(text)
                spanBuilder.setSpan(StyleSpan(Typeface.BOLD), 0, index, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                return SpannableString(spanBuilder)
            }
        }
        // If nothing else return the original text
        return SpannableString(text)
    }

    override fun getLayout(): Int = R.layout.layout_pinned_message

    override fun initializeViewBinding(view: View): LayoutPinnedMessageBinding =
        LayoutPinnedMessageBinding.bind(view)
}