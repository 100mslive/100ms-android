package live.hms.roomkit.ui.polls

import androidx.lifecycle.ViewModel

data class PollCreationInfo(
    val timer : Boolean = false,
    val anon : Boolean = false,
    val hideVote : Boolean = false,
    val isPoll  : Boolean = false,
    val pollTitle : String = "",
)

class PollsViewModel : ViewModel() {

    private var pollCreationInfo = PollCreationInfo()

    fun setTitle(title : String) {
        pollCreationInfo = pollCreationInfo.copy(pollTitle = title)
    }
    fun setTimer(timerEnabled : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(timer = timerEnabled)
    }
    fun isAnon(isAnon : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(anon = isAnon)
    }
    fun markHideVoteCount(isHidden : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(hideVote = isHidden)
    }

    fun setPollOrQuiz(isPoll : Boolean) {
        pollCreationInfo = pollCreationInfo.copy(isPoll = isPoll)
    }

    fun getPollsCreationInfo() = pollCreationInfo

    fun isPoll() = pollCreationInfo.isPoll
}