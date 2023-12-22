package live.hms.roomkit.ui.meeting.participants

import android.view.View
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import live.hms.roomkit.ui.meeting.AllowedToMessageParticipants
import live.hms.video.sdk.models.HMSPeer

class ChatRecipientSearchUseCase(private val updateList : suspend () -> Unit) {
    private var filterText : String? = null
    fun isSearching() = !filterText.isNullOrEmpty()
    fun setSearchVisibility(textInputSearch : EditText,
                            allowedToMessageParticipants: AllowedToMessageParticipants) {
        textInputSearch.visibility = if(allowedToMessageParticipants.peers) View.VISIBLE else View.GONE
    }
    fun initSearchView(textInputSearch : EditText, scope : LifecycleCoroutineScope) {
        textInputSearch.apply {
            addTextChangedListener { text ->
                scope.launch {
                    filterText = text.toString()
                    updateList()
                }
            }
        }
    }

    fun getFilteredPeers(remotePeers : List<HMSPeer>) : List<HMSPeer> {
        val filterText = filterText ?: return remotePeers
        return remotePeers.filter { it.name.contains(filterText) }
    }
}