package live.hms.app2.ui.meeting.participants

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import live.hms.app2.databinding.ListItemPeerListBinding
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer
import live.hms.video.sdk.models.role.HMSRole
import java.util.*
import kotlin.collections.ArrayList

class ParticipantsAdapter(
  private val availableRoles: () -> List<HMSRole>,
  private val changeRole: (remotePeer: HMSRemotePeer, toRole: HMSRole) -> Unit,
) : RecyclerView.Adapter<ParticipantsAdapter.PeerViewHolder>() {

  companion object {
    private const val TAG = "ParticipantsAdapter"
  }

  private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

  inner class PeerViewHolder(
    private val binding: ListItemPeerListBinding
  ) : RecyclerView.ViewHolder(binding.root), AdapterView.OnItemSelectedListener {
    init {
      with(binding) {
        Log.d(TAG, "SizeRoles: checking")
        val roles = availableRoles()
        Log.d(TAG, "SizeRoles: ${roles.size}")
        val a = 2
        ArrayAdapter<CharSequence>(
          root.context,
          android.R.layout.simple_list_item_1,
          availableRoles().map { it.name })
          .also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            participantChangeRoleSpinner.adapter = adapter
          }

//        ArrayAdapter.createFromResource(
//          root.context,
//          R.array.custom_role_names,
//          android.R.layout.simple_list_item_1
//        ).also { adapter ->
//          // Specify the layout to use when the list of choices appears
//          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//          // Apply the adapter to the spinner
//          participantChangeRoleSpinner.adapter = adapter
//        }
        participantChangeRoleSpinner.onItemSelectedListener = this@PeerViewHolder
        participantKick.setOnClickListener { Log.d(TAG, "${items[adapterPosition]} kick!") }
        participantMuteToggle.setOnClickListener { Log.d(TAG, "${items[adapterPosition]} mute!") }
      }

    }

    fun bind(item: HMSPeer) {
      with(binding) {
        name.text = item.name
        iconScreenShare.visibility = v(item.auxiliaryTracks.isNotEmpty())
        iconAudioOff.visibility = v(item.audioTrack?.isMute != false)
        iconVideoOff.visibility = v(item.videoTrack?.isMute != false)
      }

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      Log.d(TAG, "Role $position selected for ${items[position]} is $id with position $position")
//      changeRole(items[position], availableRoles().find { it.name })
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
      Log.d(TAG, "Nothing selected")
    }

  }

  private val items = ArrayList<HMSPeer>()

  @MainThread
  fun setItems(newItems: Array<HMSPeer>) {
    items.clear()
    items.addAll(newItems)
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
    val binding = ListItemPeerListBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return PeerViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
    holder.bind(items[position])
  }

  override fun getItemCount(): Int = items.size
}