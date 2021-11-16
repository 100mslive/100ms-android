package live.hms.app2.ui.meeting

import com.google.gson.Gson

data class CustomPeerMetadata(
    val isHandRaised: Boolean,
    val name: String
) {

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(metadata: String): CustomPeerMetadata? {
            return Gson().fromJson(metadata, CustomPeerMetadata::class.java)
        }
    }

}