package live.hms.roomkit.ui.meeting.chat

import live.hms.video.sdk.models.role.HMSRole

/**
 * Test helper class for RolePresenter
 * This represents a role in the UI layer
 */
data class RolePresenter(
    val hmsRole: HMSRole
) {
    override fun toString(): String = hmsRole.name
}