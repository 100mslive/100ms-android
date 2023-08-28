package live.hms.roomkit.ui.meeting.participants
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.common.util.helpers.NetworkQualityHelper
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.connection.stats.quality.HMSNetworkQuality
import live.hms.video.media.tracks.HMSTrackType
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer

class ParticipantItem(private val hmsPeer: HMSPeer,
                      private val toggleTrack: (hmsPeer: HMSRemotePeer, type: HMSTrackType) -> Unit,
                      private val changeRole: (remotePeerId: String) -> Unit,
                      private val isAllowedToChangeRole : Boolean,
                      private val isAllowedToMutePeers : Boolean,
                      private val isAllowedToRemovePeers : Boolean
                      ) : BindableItem<ListItemPeerListBinding>(){
    override fun bind(viewBinding: ListItemPeerListBinding, position: Int) {
        viewBinding.name.text = hmsPeer.name
        updateNetworkQuality(hmsPeer.networkQuality, viewBinding)
        updateHandRaise(hmsPeer.metadata, viewBinding)
        // Don't show the settings if they aren't allowed to change anything at all.
        viewBinding.peerSettings.visibility = if(hmsPeer.isLocal || !(isAllowedToMutePeers || isAllowedToChangeRole || isAllowedToRemovePeers))
            View.GONE
        else View.VISIBLE

        viewBinding.peerSettings.setOnClickListener {
            with(PopupMenu(viewBinding.root.context, viewBinding.peerSettings)) {
                inflate(getMenuForGroup(hmsPeer))
                setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId) {
                        R.id.remove_from_stage -> {
                            // TODO role change to WHAT? Either guest or hls-viewer
                            changeRole(hmsPeer.peerID)
                            true
                        }

                        R.id.toggle_audio -> {
                            // Toggle audio
                            toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.AUDIO)
                            true
                        }
                        R.id.toggle_video -> {
                            // Toggle video
                            toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.VIDEO)
                            true
                        }

                        else -> false
                    }
                }
                show()
            }
        }
    }

    private fun getMenuForGroup(peer: HMSPeer): Int {
        val isHandRaised = CustomPeerMetadata.fromJson(peer.metadata)?.isHandRaised == true
                && peer.hmsRole.name.lowercase() != "broadcaster" && peer.hmsRole.name.lowercase() != "host"

        return if(isHandRaised)
            R.menu.menu_participant_hand_raise
        else
            when(peer.hmsRole.name.lowercase()) {
                "broadcaster" -> R.menu.menu_broadcaster
    //                "hls-viewer" -> R.menu
                else -> R.menu.menu_participant
            }
    }

    private fun updateHandRaise(metadata: String, viewBinding: ListItemPeerListBinding) {
        val isHandRaised = CustomPeerMetadata.fromJson(metadata)?.isHandRaised == true
        viewBinding.handraise.visibility = if(isHandRaised)
            View.VISIBLE
        else
            View.GONE
    }

    private fun updateNetworkQuality(
        networkQuality: HMSNetworkQuality?,
        viewBinding: ListItemPeerListBinding
    ) {
        val downlinkSpeed = networkQuality?.downlinkQuality ?: -1
        val imageView = viewBinding.badNetworkIndicator
        NetworkQualityHelper.getNetworkResource(downlinkSpeed, viewBinding.root.context).let { drawable ->
            if (downlinkSpeed == 0) {
                imageView.setColorFilter(getColorOrDefault(HMSPrebuiltTheme.getColours()?.alertErrorDefault, HMSPrebuiltTheme.getDefaults().error_default), android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                imageView.colorFilter = null
            }
            imageView.setImageDrawable(drawable)
            if (drawable == null){
                imageView.visibility = View.GONE
            }else{
                imageView.visibility = View.VISIBLE
            }
        }
    }


    override fun getLayout(): Int = R.layout.list_item_peer_list

    override fun initializeViewBinding(view: View): ListItemPeerListBinding =
        ListItemPeerListBinding.bind(view)

}