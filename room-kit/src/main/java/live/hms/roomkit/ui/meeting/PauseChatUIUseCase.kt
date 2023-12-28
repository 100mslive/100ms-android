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

}