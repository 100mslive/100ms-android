package live.hms.roomkit.ui.meeting.participants
import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.connection.stats.quality.HMSNetworkQuality
import live.hms.video.sdk.models.HMSPeer

class ParticipantItem(private val hmsPeer : HMSPeer) : BindableItem<ListItemPeerListBinding>(){
    override fun bind(viewBinding: ListItemPeerListBinding, position: Int) {
        viewBinding.name.text = hmsPeer.name
        updateNetworkQuality(hmsPeer.networkQuality, viewBinding)
        updateHandRaise(hmsPeer.metadata, viewBinding)
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