package live.hms.app2.ui.meeting.participants

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import live.hms.app2.databinding.DialogParticipantsBinding


class ParticipantsDialog(context: Context) : Dialog(context) {

    var adapter: ParticipantsAdapter
    private lateinit var dialogParticipantsBinding: DialogParticipantsBinding
    var participantCount: Int = 0
        set(value) {
            if (this.isShowing) {
                dialogParticipantsBinding.participantCount.text = value.toString()
                field = value
            }
        }

    init {
        adapter = ParticipantsAdapter(false, false, false, false, {}, VIEW_TYPE.PREVIEW)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogParticipantsBinding = DialogParticipantsBinding.inflate(LayoutInflater.from(context))
        setContentView(dialogParticipantsBinding.root)

        initViews()
    }

    private fun initViews() {
        dialogParticipantsBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ParticipantsDialog.adapter
        }
    }
}