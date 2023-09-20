package live.hms.roomkit.ui.meeting

import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSRoomLayout

class PrebuiltInfoContainer(private val hmssdk: HMSSDK) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    private val localPeer by lazy { hmssdk.getLocalPeer()!! }

    fun isChatEnabled(): Boolean =
        // how do we even know if it's in hls? What if they have both?
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
                ?.elements?.chat != null ||
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                ?.default?.elements?.chat != null

    fun chatInitialStateOpen(): Boolean {
        val isChatInitialOpen =
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements?.chat?.initialState == "CHAT_STATE_OPEN"
                    ||
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                ?.default?.elements?.chat?.initialState == "CHAT_STATE_OPEN"

        // Initial open is only valid for overlay chat
        return isChatOverlay() && isChatInitialOpen
    }
    fun isChatOverlay() =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements?.chat?.overlayView == true
                ||
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing
            ?.default?.elements?.chat?.overlayView == true


    fun onStageExp(role : String) =
        roleMap[role]?.screens?.conferencing?.default?.elements?.onStageExp

    fun offStageRoles(role : String) =
        roleMap[role]?.screens?.conferencing?.default?.elements?.onStageExp?.offStageRoles

    fun setParticipantLabelInfo(hmsRoomLayout: HMSRoomLayout?){
        this.hmsRoomLayout = hmsRoomLayout
        hmsRoomLayout?.data
            ?.forEach {data ->
                data?.role?.let {
                    roleMap[it] = data
                    //Log.d("Participant","$it, $data")
                }

            }
    }
}