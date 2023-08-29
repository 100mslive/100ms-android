package live.hms.roomkit.ui.meeting.participants
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.ParticipantLabelInfo
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.connection.stats.quality.HMSNetworkQuality
import live.hms.video.media.tracks.HMSTrackType
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer

class ParticipantItem(private val hmsPeer: HMSPeer,
                      private val viewerPeer : HMSLocalPeer,
                      private val toggleTrack: (hmsPeer: HMSRemotePeer, type: HMSTrackType) -> Unit,
                      private val changeRole: (remotePeerId: String, roleToChangeTo : String) -> Unit,
                      private val isAllowedToChangeRole : Boolean,
                      private val isAllowedToMutePeers : Boolean,
                      private val isAllowedToRemovePeers : Boolean,
                      private val participantLabelInfo : ParticipantLabelInfo
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
                // Hide bring on stage if it's not a broadcaster looking at it.
                menu.findItem(R.id.bring_on_stage)?.isVisible = viewerPeer.hmsRole.name == "broadcaster"
                setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId) {
                        R.id.bring_on_stage -> {
                            // You must have a role to bring on stage
                            val role = participantLabelInfo.onStageExp(viewerPeer.hmsRole.name)?.onStageRole
                            if(role != null)
                                changeRole(hmsPeer.peerID, role)
                            true
                        }
                        R.id.remove_from_stage -> {
                            // TODO What role is supposed to be picked for offstage
                            val role = participantLabelInfo.onStageExp(viewerPeer.hmsRole.name)?.offStageRoles?.firstOrNull()
                            if(role != null)
                                changeRole(hmsPeer.peerID, role)
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

    private fun getMenuForGroup(forPeer: HMSPeer): Int {
        val isHandRaised = CustomPeerMetadata.fromJson(forPeer.metadata)?.isHandRaised == true
                && forPeer.hmsRole.name.lowercase() != "broadcaster" && forPeer.hmsRole.name.lowercase() != "host"

        return if(isHandRaised)
            R.menu.menu_participant_hand_raise
        else
            when(forPeer.hmsRole.name.lowercase()) {
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