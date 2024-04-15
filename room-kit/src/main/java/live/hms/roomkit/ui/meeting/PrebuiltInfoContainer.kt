package live.hms.roomkit.ui.meeting

import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.video.sdk.HMSSDK
import live.hms.video.signal.init.HMSRoomLayout
import live.hms.video.sdk.models.TranscriptionState
class PrebuiltInfoContainer(private val hmssdk: HMSSDK) {
    private var hmsRoomLayout : HMSRoomLayout? = null
    private val roleMap : MutableMap<String, HMSRoomLayout.HMSRoomLayoutData> = mutableMapOf()
    private val localPeer by lazy { hmssdk.getLocalPeer()!! }

    fun shouldForceRoleChange() : Boolean = onStageExp(localPeer.hmsRole.name)?.skipPreviewForRoleChange == true
    fun isAllowedToBlockUserFromChat() : Boolean =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.realTimeControls?.canBlockUser == true ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.realTimeControls?.canBlockUser == true

    fun isAllowedToPinMessages() : Boolean =
        roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.allowPinningMessages == true ||
                roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                    ?.default?.elements?.chat?.allowPinningMessages == true

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
        val recipient = allowedToMessageWhatParticipants() ?: return null

        return if(recipient.everyone) {
            Recipient.Everyone
        }
        else if (recipient.roles.isNotEmpty()) {
            val localPeerRole = hmssdk.getLocalPeer()?.hmsRole?.name
            val name = recipient.roles.filter { it != localPeerRole }.firstOrNull()
            val role = hmssdk.getRoles().find { it.name == name }
            if(role != null)
                Recipient.Role(role)
            else null
        }
        else if (recipient.peers)
            null
        else null
    }

    fun allowedToMessageWhatParticipants(): AllowedToMessageParticipants? {
        // If there's no room, then the user hasn't joined either
        if (hmssdk.getRoom() == null)
            return null

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

    fun getChatTitle(): String  {
        val hlsTitle = roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
            ?.elements?.chat?.chatTitle

        val confTitle = roleMap[localPeer.hmsRole.name]?.screens?.conferencing
            ?.default?.elements?.chat?.chatTitle

        return confTitle ?: hlsTitle ?: "Chat"
    }

    fun shouldSkipPreview(): Boolean {
        return hmsRoomLayout?.data?.get(0)?.screens?.preview?.skipPreview == true
    }

    fun getLiveStreamingHeaderTitle() : String? {
        val localPeer = hmssdk.getLocalPeer()

        return if (localPeer == null) {
            hmsRoomLayout?.data?.get(0)?.screens?.conferencing?.hlsLiveStreaming?.elements
                ?.hlsLiveStreamingHeader?.title
        } else {
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements
                ?.hlsLiveStreamingHeader?.title
        }
    }

    fun getLiveStreamingHeaderDescription() : String? {
        val localPeer = hmssdk.getLocalPeer()

        return if (localPeer == null) {
            hmsRoomLayout?.data?.get(0)?.screens?.conferencing?.hlsLiveStreaming?.elements
                ?.hlsLiveStreamingHeader?.description
        } else {
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming?.elements
                ?.hlsLiveStreamingHeader?.description
        }
    }

    fun handRaiseAvailable() : Boolean {
        val localPeer = hmssdk.getLocalPeer()
        val available =  if (localPeer == null) {
            hmsRoomLayout?.data?.get(0)?.screens?.conferencing?.hlsLiveStreaming?.elements
                ?.handRaise != null
        } else {
            roleMap[localPeer.hmsRole.name]?.screens?.conferencing?.hlsLiveStreaming
                ?.elements?.handRaise != null ||
                    roleMap[localPeer.hmsRole.name]?.screens?.conferencing
                        ?.default?.elements?.handRaise != null
        }
        return available
    }

}

data class AllowedToMessageParticipants(
    val everyone : Boolean,
    val peers : Boolean,
    val roles : List<String>
) {
    fun isChatSendingEnabled() : Boolean = everyone || peers || roles.isNotEmpty()
}