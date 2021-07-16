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
  private val availableRoles: List<HMSRole>,
  private val changeRole: (remotePeer: HMSRemotePeer, toRole: HMSRole) -> Unit,
) : RecyclerView.Adapter<ParticipantsAdapter.PeerViewHolder>() {

  private val availableRoleStrings = availableRoles.map { it.name }
  companion object {
    private const val TAG = "ParticipantsAdapter"
  }

  private fun v(value: Boolean) = if (value) View.VISIBLE else View.GONE

  inner class PeerViewHolder(
    private val binding: ListItemPeerListBinding
  ) : RecyclerView.ViewHolder(binding.root), AdapterView.OnItemSelectedListener {
    init {
      with(binding) {

        ArrayAdapter(
          root.context,
          android.R.layout.simple_list_item_1,
          availableRoleStrings
        )
          .also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            participantChangeRoleSpinner.adapter = adapter
          }
        // Set the listener without triggering it
        with(participantChangeRoleSpinner) {
          //isSelected = false
          //setSelection(0, true)
          onItemSelectedListener = this@PeerViewHolder
        }
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
        participantChangeRoleSpinner.setSelection(availableRoleStrings.indexOf(item.name))
        remotePeerOptions.visibility = if (item.isLocal) View.GONE else View.VISIBLE
      }

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      if (items[adapterPosition] is HMSRemotePeer) {
        changeRole(items[adapterPosition] as HMSRemotePeer, availableRoles[position])
        Log.d("catsas", "Running the change role to ${availableRoles[position]}")
      }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
      Log.d("catsas", "NO change role item selected")
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