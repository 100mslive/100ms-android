package live.hms.roomkit.ui.meeting

import live.hms.roomkit.ui.meeting.chat.Recipient
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
                }

            }
    }
    fun defaultRecipientToMessage() : Recipient? {
        val recipient =  allowedToMessageWhatParticipants()
        return if(recipient.everyone) {
            Recipient.Everyone
        }
        else if (recipient.roles.isNotEmpty()) {
            val name = recipient.roles.first()
            val role = hmssdk.getRoles().find { it.name == name }
            if(role != null)
                Recipient.Role(role)
            else null
        }
        else if (recipient.peers)
            null
        else null
    }

    fun allowedToMessageWhatParticipants(): AllowedToMessageParticipants {
        val everyone = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.publicChatEnabled == true ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.publicChatEnabled == true

        val peerLevelDms = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.privateChatEnabled == true ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.privateChatEnabled == true

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

    fun isAllowedToHideMessages(): Boolean =
    roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
    ?.elements?.chat?.realTimeControls?.canHideMessage == true ||
    roleMap[localPeer.hmsRole.name]?.screens?.conferencing
    ?.default?.elements?.chat?.realTimeControls?.canHideMessage == true

}

data class AllowedToMessageParticipants(
    val everyone : Boolean,
    val peers : Boolean,
    val roles : List<String>
)