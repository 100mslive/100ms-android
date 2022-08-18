package live.hms.app2.ui.meeting.participants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.DialogParticipantsBinding


class ParticipantsDialog : BottomSheetDialogFragment() {

    var adapter: ParticipantsAdapter
    private lateinit var dialogParticipantsBinding: DialogParticipantsBinding
    var participantCount: Int = 0
        set(value) {
            if (this.isVisible) {
                dialogParticipantsBinding.participantCount.text = value.toString()
            }
            field = value
        }

    init {
        adapter = ParticipantsAdapter(false, false, false, false, {}, VIEW_TYPE.PREVIEW)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialogParticipantsBinding = DialogParticipantsBinding.inflate(LayoutInflater.from(context))
        initViews()
        return dialogParticipantsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogParticipantsBinding.participantCount.text = participantCount.toString()
    }

    private fun initViews() {
        dialogParticipantsBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ParticipantsDialog.adapter
        }
    }
}