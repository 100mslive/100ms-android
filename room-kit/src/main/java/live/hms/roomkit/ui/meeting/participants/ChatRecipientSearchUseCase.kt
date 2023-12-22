package live.hms.roomkit.ui.meeting.participants

import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ChatRecipientSearchUseCase {
    private var filterText : String? = null
    private fun isSearching() = !filterText.isNullOrEmpty()
    fun initSearchView(textInputSearch : TextInputEditText, scope : LifecycleCoroutineScope) {
        textInputSearch.apply {
            addTextChangedListener { text ->
                scope.launch {
                    filterText = text.toString()
//                    updateParticipantsAdapter(getAllPeers())
                    // TODO update peer

                }
            }
        }
    }
}