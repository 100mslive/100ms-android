package live.hms.roomkit.ui.meeting

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.card.MaterialCardView
import live.hms.roomkit.setOnSingleClickListener
import live.hms.video.sdk.models.HMSLocalPeer

class PauseChatUIUseCase {
    fun setChatPauseVisible(materialCardView: MaterialCardView,
                            meetingViewModel: MeetingViewModel) {
        materialCardView.visibility = if(meetingViewModel.isAllowedToPauseChat()) View.VISIBLE else View.GONE
        materialCardView.setOnSingleClickListener {
            val newState = meetingViewModel.chatPauseState.value!!
            meetingViewModel.pauseChat(
                newState.copy(enabled = !newState.enabled,
                    updatedBy = meetingViewModel.hmsSDK.getLocalPeer()?.name ?: "Participant"
                    )
            )
        }
    }

    fun observeChatPauseState(viewLifecycleOwner: LifecycleOwner, meetingViewModel: MeetingViewModel) {
        meetingViewModel.chatPauseState.observe(viewLifecycleOwner) { chatState ->
            // Set the chat as paused (can use the block UI I guess)

        }
    }
}