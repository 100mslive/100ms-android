package live.hms.roomkit.ui.meeting

import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.signal.init.HMSRoomLayout

class PrebuiltInfoContainer(private val hmssdk: HMSSDK) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    private val localPeer by lazy { hmssdk.getLocalPeer()!! }
    fun chatInitialStateOpen() : Boolean = roleMap[localPeer.hmsRole.name]?.screens?.conferencing
        ?.default?.elements?.chat?.initialState == "CHAT_STATE_OPEN"
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