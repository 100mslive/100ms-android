package live.hms.roomkit.ui.meeting.participants

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.View
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutPinnedMessageBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.SessionMetadataUseCase
import live.hms.roomkit.ui.theme.applyTheme

class PinnedMessageItem(val receivedPinnedMessage: SessionMetadataUseCase.PinnedMessage,
    )
    : BindableItem<LayoutPinnedMessageBinding>(receivedPinnedMessage.hashCode().toLong()), ExpandableItem {
    private lateinit var expand : ExpandableGroup
    override fun bind(viewBinding: LayoutPinnedMessageBinding, position: Int) {
        with(viewBinding) {
            applyTheme()
            pinnedMessage.text = boldTheSenderName(receivedPinnedMessage.text)
            root.setOnSingleClickListener {
                expand.onToggleExpanded()
                pinnedMessage.maxLines = if(expand.isExpanded) {
                    100
                } else
                    2
            }
        }
    }

    private fun boldTheSenderName(text : String?) : SpannableString {
        if(text == null) return SpannableString("")
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

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        this.expand = onToggleListener
    }

}