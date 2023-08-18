package live.hms.roomkit.ui.meeting.participants
import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.video.sdk.models.HMSPeer

class ParticipantItem(val hmsPeer : HMSPeer) : BindableItem<ListItemPeerListBinding>(){
    override fun bind(viewBinding: ListItemPeerListBinding, position: Int) {
        viewBinding.name.text = hmsPeer.name
    }

    override fun getLayout(): Int = R.layout.list_item_peer_list

    override fun initializeViewBinding(view: View): ListItemPeerListBinding =
        ListItemPeerListBinding.bind(view)

}