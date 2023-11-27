package live.hms.roomkit.ui.meeting

import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSRoomLayout

class PrebuiltInfoContainer(private val hmssdk: HMSSDK) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    private val localPeer by lazy { hmssdk.getLocalPeer()!! }

    fun isAllowedToBlockUserFromChat() : Boolean =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.realTimeControls?.canBlockUser != null ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.realTimeControls?.canBlockUser != null

    fun isAllowedToPinMessages() : Boolean =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.allowPinningMessages != null ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.allowPinningMessages != null

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

    fun offStageRoles(role : String) : List<String?>? =
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

    fun allowedToMessageWhatParticipants(): AllowedToMessageParticipants {
        val everyone = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.publicChatEnabled != null ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.publicChatEnabled != null

        val peerLevelDms = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.privateChatEnabled != null ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.privateChatEnabled != null

        val whitelistedRolesConference = roleMap[localPeer.hmsRole.name]?.screens?.conferencing
            ?.default?.elements?.chat?.rolesWhiteList ?: emptyList()

        val whitelistedRolesHls = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.rolesWhiteList ?: emptyList()

        return AllowedToMessageParticipants(everyone, peerLevelDms, whitelistedRolesConference + whitelistedRolesHls)
    }

    fun isAllowedToPauseChat(): Boolean =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.realTimeControls?.canDisableChat == true ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.realTimeControls?.canDisableChat == true

}

data class AllowedToMessageParticipants(
    val everyone : Boolean,
    val peers : Boolean,
    val roles : List<String>
)