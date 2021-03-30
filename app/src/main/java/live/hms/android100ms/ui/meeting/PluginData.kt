package live.hms.android100ms.ui.meeting

import live.hms.video.payload.HMSPayloadData

enum class PluginType {
    WHITEBOARD,
    DRIVE
}

data class PluginData(
        val type: PluginType,
        val url: String,
        val ownerName: String,
        val ownerUid: String
)

