package live.hms.roomkit.ui.meeting.participants
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.roomkit.R
import live.hms.roomkit.databinding.CustomMenuLayoutBinding
import live.hms.roomkit.databinding.ListItemPeerListBinding
import live.hms.roomkit.gone
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.show
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.PrebuiltInfoContainer
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.video.connection.stats.quality.HMSNetworkQuality
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSAudioTrack
import live.hms.video.media.tracks.HMSTrackType
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRemotePeer
import live.hms.video.sdk.models.HMSSpeaker
import org.webrtc.ContextUtils.getApplicationContext

class ParticipantItem(
    private val hmsPeer: HMSPeer,
    private val viewerPeer: HMSLocalPeer,
    private val toggleTrack: (hmsPeer: HMSRemotePeer, type: HMSTrackType) -> Unit,
    private val changeRole: (remotePeerId: String, roleToChangeTo: String, force: Boolean) -> Unit,
    private val isAllowedToChangeRole: Boolean,
    private val isAllowedToMutePeers: Boolean,
    private val isAllowedToRemovePeers: Boolean,
    private val prebuiltInfoContainer: PrebuiltInfoContainer,
    private val participantPreviousRoleChangeUseCase: ParticipantPreviousRoleChangeUseCase,
    private val requestPeerLeave: (hmsPeer: HMSRemotePeer, reason: String) -> Unit,
    private val activeSpeakers: LiveData<Pair<List<MeetingTrack>, Array<HMSSpeaker>>>
) : BindableItem<ListItemPeerListBinding>(){
    override fun bind(viewBinding: ListItemPeerListBinding, position: Int) {
        viewBinding.applyTheme()
        val name = if(hmsPeer.isLocal){
            "${hmsPeer.name} (You)"
        } else {
            hmsPeer.name
        }
        viewBinding.name.text = name
        updateNetworkQuality(hmsPeer.networkQuality, viewBinding)
        updateHandRaise(hmsPeer.metadata, viewBinding)
        updateSpeaking(hmsPeer.audioTrack, viewBinding)
        // Don't show the settings if they aren't allowed to change anything at all.
        viewBinding.peerSettings.visibility = if(hmsPeer.isLocal || !(isAllowedToMutePeers || isAllowedToChangeRole || isAllowedToRemovePeers))
            View.GONE
        else View.VISIBLE

        viewBinding.peerSettings.setOnClickListener {
            val inflater: LayoutInflater = getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val view = inflater.inflate(R.layout.custom_menu_layout, null)
            val popBinding = CustomMenuLayoutBinding.bind(view)

            popBinding.applyTheme(getMenuOptions(hmsPeer))
            val mypopupWindow = PopupWindow(view, view.context.resources.getDimension(R.dimen.popup_width).toInt(), view.context.resources.getDimension(R.dimen.twohundred_dp).toInt(), true)
            mypopupWindow.showAsDropDown(it)
            mypopupWindow.contentView.setOnClickListener {
                mypopupWindow.dismiss()
            }
            val options = getMenuOptions(hmsPeer)
            with(options) {
//                val bringOnStage : Boolean,
                popBinding.onStage.visibility = if(bringOnStage || bringOffStage) View.VISIBLE else View.GONE
                if(bringOnStage) {
                    popBinding.onStage.text = "Bring OnStage"
                    popBinding.onStage.setOnClickListener {
                        participantPreviousRoleChangeUseCase.setPreviousRole(hmsPeer, object :
                            HMSActionResultListener {
                                override fun onError(error: HMSException) {
                                    // Throw error
                                    Log.d("BringOnStageError","$error")
                                }

                                override fun onSuccess() {
                                    val role = prebuiltInfoContainer.onStageExp(viewerPeer.hmsRole.name)?.onStageRole
                                    if(role != null)
                                        changeRole(hmsPeer.peerID, role, false)
                                }
                            })
                        mypopupWindow.dismiss()
                    }
                }
                if(bringOffStage){
                    popBinding.onStage.text = "Remove From Stage"
                    popBinding.onStage.setOnClickListener {
                        val role = participantPreviousRoleChangeUseCase.getPreviousRole(hmsPeer)
                            Log.d("RolesChangingTo","$role")
                            if(role != null)
                                changeRole(hmsPeer.peerID, role, true)
                        mypopupWindow.dismiss()
                    }
                }
                popBinding.toggleAudio.visibility = if(audioIsOn != null && showToggleAudio) View.VISIBLE else View.GONE
                if(audioIsOn == true)
                    popBinding.toggleAudio.text = "Mute Audio"
                else if (audioIsOn == false){
                    popBinding.toggleAudio.text = "Unmute Audio"
                }
                popBinding.toggleAudio.setOnClickListener {
                    toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.AUDIO)
                    mypopupWindow.dismiss()
                }

                popBinding.toggleVideo.visibility = if(videoIsOn != null && showToggleVideo) View.VISIBLE else View.GONE
                if(videoIsOn == true) {
                    popBinding.toggleVideo.text = "Mute Video"
                }
                else if (videoIsOn == false){
                    popBinding.toggleVideo.text = "Unmute Video"
                }
                popBinding.toggleVideo.setOnClickListener {
                    toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.VIDEO)
                    mypopupWindow.dismiss()
                }

                popBinding.raiseHand.visibility = View.GONE//if(lowerHand) View.VISIBLE else View.GONE
                popBinding.removeParticipant.visibility = if(removeParticipant) View.VISIBLE else View.GONE
                popBinding.removeParticipant.setOnClickListener {
                    requestPeerLeave(hmsPeer as HMSRemotePeer, "Exit")
                    mypopupWindow.dismiss()
                }
            }

//            with(PopupMenu(viewBinding.root.context, viewBinding.peerSettings)) {
//                setForceShowIcon(true)
//                inflate(getMenuForGroup(hmsPeer))
//                // Hide bring on stage if it's not a broadcaster looking at it.
//                menu.findItem(R.id.bring_on_stage)?.isVisible = viewerPeer.hmsRole.name == "broadcaster"
//                setOnMenuItemClickListener { menuItem ->
//                    when(menuItem.itemId) {
//                        R.id.bring_on_stage -> {
//                            // You must have a role to bring on stage
//                            participantPreviousRoleChangeUseCase.setPreviousRole(hmsPeer, object :HMSActionResultListener {
//                                override fun onError(error: HMSException) {
//                                    // Throw error
//                                    Log.d("BringOnStageError","$error")
//                                }
//
//                                override fun onSuccess() {
//                                    val role = prebuiltInfoContainer.onStageExp(viewerPeer.hmsRole.name)?.onStageRole
//                                    if(role != null)
//                                        changeRole(hmsPeer.peerID, role, false)
//                                }
//                            })
//
//                            true
//                        }
//                        R.id.remove_from_stage -> {
//                            val role = participantPreviousRoleChangeUseCase.getPreviousRole(hmsPeer)
//                            Log.d("RolesChangingTo","$role")
//                            if(role != null)
//                                changeRole(hmsPeer.peerID, role, true)
//                            true
//                        }
//
//                        R.id.toggle_audio -> {
//                            // Toggle audio
//                            toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.AUDIO)
//                            true
//                        }
//                        R.id.toggle_video -> {
//                            // Toggle video
//                            toggleTrack(hmsPeer as HMSRemotePeer, HMSTrackType.VIDEO)
//                            true
//                        }
//
//                        else -> false
//                    }
//                }
//                show()
//            }
        }
    }

    private fun updateSpeaking(audioTrack: HMSAudioTrack?, viewBinding: ListItemPeerListBinding) {
        if (audioTrack?.isMute == true || audioTrack?.isMute == null) {
            // Mute
            viewBinding.muteUnmuteIcon.show()
            viewBinding.audioLevelView.gone()
            viewBinding.muteUnmuteIcon.setImageDrawable(ResourcesCompat.getDrawable(viewBinding.root.resources, R.drawable.ic_audio_toggle_off, null))
        }
        else {
            viewBinding.muteUnmuteIcon.gone()
            viewBinding.audioLevelView.show()
            viewBinding.audioLevelView.requestLayout()
            activeSpeakers.removeObservers(viewBinding.root.context as LifecycleOwner)
            activeSpeakers.observe(viewBinding.root.context as LifecycleOwner) { (t, speakers) ->
                val level = speakers.find { it.hmsTrack?.trackId == audioTrack.trackId }?.level ?: 0
                viewBinding.audioLevelView.update(level)
            }
        }

    }

    private fun getMenuOptions(forPeer: HMSPeer) : EnabledMenuOptions {
        val isOffStageRole =
            prebuiltInfoContainer.onStageExp("broadcaster")?.offStageRoles?.contains(
                forPeer.hmsRole.name
            ) == true
        val isOnStageButNotBroadcasterRole = prebuiltInfoContainer.onStageExp("broadcaster")?.onStageRole == forPeer.hmsRole.name

        val isHandRaised = CustomPeerMetadata.fromJson(forPeer.metadata)?.isHandRaised == true
                // You have to be in the offstage roles to be categorized as hand raised
                && isOffStageRole

        return EnabledMenuOptions(
            bringOnStage = isOffStageRole,
            bringOffStage = isOnStageButNotBroadcasterRole,
            lowerHand = isHandRaised,
            removeParticipant = isAllowedToRemovePeers,
            toggleMedia = isAllowedToMutePeers,
            audioIsOn = if(!isAllowedToMutePeers) null else hmsPeer.audioTrack?.isMute == false,
            videoIsOn = if(!isAllowedToMutePeers) null else hmsPeer.videoTrack?.isMute == false,
            showToggleAudio = hmsPeer.hmsRole.publishParams?.allowed?.contains("audio") == true,
            showToggleVideo  = hmsPeer.hmsRole.publishParams?.allowed?.contains("video") == true
        )
    }
//    private fun getMenuForGroup(forPeer: HMSPeer): Int {
//        val isOffStageRole =
//            prebuiltInfoContainer.onStageExp("broadcaster")?.offStageRoles?.contains(
//                forPeer.hmsRole.name
//            ) == true
//        val isOnStageButNotBroadcasterRole = prebuiltInfoContainer.onStageExp("broadcaster")?.onStageRole == forPeer.hmsRole.name
//
//        val isHandRaised = CustomPeerMetadata.fromJson(forPeer.metadata)?.isHandRaised == true
//                // You have to be in the offstage roles to be categorized as hand raised
//                && isOffStageRole
//
//        return if (isHandRaised)
//            R.menu.menu_participant_hand_raise
//        else if (isOffStageRole) {
//            R.menu.menu_participants_all
//        } else if(isOnStageButNotBroadcasterRole) {
//            R.menu.menu_participant_onstage_not_broadcaster
//        }
//        else {
//            R.menu.menu_broadcaster
//        }
//    }

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