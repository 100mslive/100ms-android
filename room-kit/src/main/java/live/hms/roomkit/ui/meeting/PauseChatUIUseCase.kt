package live.hms.roomkit.ui.meeting

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.card.MaterialCardView
import live.hms.roomkit.setOnSingleClickListener

class PauseChatUIUseCase {
    fun setChatPauseVisible(
        materialCardView: MaterialCardView,
        meetingViewModel: MeetingViewModel
    ) {
        materialCardView.visibility = View.GONE//if(meetingViewModel.isAllowedToPauseChat()) View.VISIBLE else View.GONE
        materialCardView.setOnSingleClickListener {
            meetingViewModel.togglePauseChat()
        }
    }

    fun observeChatPauseState(viewLifecycleOwner: LifecycleOwner, meetingViewModel: MeetingViewModel) {
        meetingViewModel.chatPauseState.observe(viewLifecycleOwner) { chatState ->
            // Set the chat as paused (can use the block UI I guess)

        }
    }
}