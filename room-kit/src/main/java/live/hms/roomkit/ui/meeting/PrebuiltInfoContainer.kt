package live.hms.roomkit.ui.meeting

import android.util.Log
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSLocalPeer
import live.hms.video.signal.init.HMSRoomLayout

class PrebuiltInfoContainer(private val hmssdk: HMSSDK) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    private val localPeer by lazy { hmssdk.getLocalPeer()!! }

    fun isChatEnabled(isHls : Boolean) : Boolean {
        return if(isHls) {
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
                ?.elements?.chat != null
        } else {
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                ?.default?.elements?.chat != null
        }
    }
    fun chatInitialStateOpen(isHls : Boolean) : Boolean = if(isHls) {
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements?.chat?.initialState == "CHAT_STATE_OPEN"
    } else {
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing
            ?.default?.elements?.chat?.initialState == "CHAT_STATE_OPEN"
    }
    fun isChatOverlay(isHls : Boolean) = if(isHls) {
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements?.chat?.overlayView == true
    } else {
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing
            ?.default?.elements?.chat?.overlayView == true
    }

    fun onStageExp(role : String) =
        roleMap[role]?.screens?.conferencing?.default?.elements?.onStageExp

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