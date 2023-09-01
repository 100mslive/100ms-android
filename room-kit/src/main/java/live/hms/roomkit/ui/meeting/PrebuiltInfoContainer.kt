package live.hms.roomkit.ui.meeting

import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.signal.init.HMSRoomLayout

class PrebuiltInfoContainer(private val localPeer : HMSLocalPeer) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()

    fun isChatOverlay() = roleMap[localPeer.hmsRole.name]?.screens?.conferencing
        ?.default?.elements?.chat?.overlayView == true
    fun onStageExp(role : String) =
        roleMap[role]?.screens?.conferencing?.default?.elements?.onStageExp

    fun setParticipantLabelInfo(hmsRoomLayout: HMSRoomLayout?){
        this.hmsRoomLayout = hmsRoomLayout
        hmsRoomLayout?.data
            ?.forEach {data ->
                data?.role?.let {
                    roleMap[it] = data
                }

            }
    }
}