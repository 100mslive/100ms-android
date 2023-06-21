package live.hms.app2.ui.polls

import androidx.lifecycle.ViewModel

data class PollCreationInfo(
    val timer : Boolean = false,
    val anon : Boolean = false,
    val hideVote : Boolean = false,
    val isPoll  : Boolean = false,
)

class PollsViewModel : ViewModel() {

    private var pollCreationInfo = PollCreationInfo()


    fun setTimer(timerEnabled : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(timer = timerEnabled)
    }
    fun isAnon(isAnon : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(anon = isAnon)
    }
    fun markHideVoteCount(isHidden : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(hideVote = isHidden)
    }

    fun highlightPollOrQuiz(isPoll : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(isPoll = isPoll)
    }
}