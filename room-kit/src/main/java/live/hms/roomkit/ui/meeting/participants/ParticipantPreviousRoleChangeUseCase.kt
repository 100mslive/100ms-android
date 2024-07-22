package live.hms.roomkit.ui.meeting.participants

import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.models.HMSPeer

class ParticipantPreviousRoleChangeUseCase(private val changeMetadata: (String, HMSActionResultListener) -> Unit) {
    fun getPreviousRole(peer: HMSPeer) : String? =
        CustomPeerMetadata.fromJson(peer.metadata)?.prevRole

    fun setPreviousRole(
        peer: HMSPeer,
        roleName: String?,
        hmsActionResultListener: HMSActionResultListener
    ) {
        val existingMetadata = CustomPeerMetadata.fromJson(peer.metadata)
        // Set the role or create a new metadata object with it.
        val updatedMetadata = existingMetadata?.copy(prevRole = roleName,
            name = peer.name,
            isBRBOn = false)
            ?: CustomPeerMetadata(
                isBRBOn = false,
                name = peer.name,
                prevRole = roleName
            )

        changeMetadata(updatedMetadata.toJson(), hmsActionResultListener)
    }
}