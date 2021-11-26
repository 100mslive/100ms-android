package live.hms.app2.ui.meeting

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class CustomPeerMetadata(
    val isHandRaised: Boolean,
    val name: String
) {

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(metadata: String?): CustomPeerMetadata? {
            return if (metadata == null) {
                null
            } else {
                return try {
                    Gson().fromJson(metadata, CustomPeerMetadata::class.java)
                } catch (ex: JsonSyntaxException) {
//                    If there's any error during deserialization just return null
//                    If you're a developer implementing this, and all your json strings
//                      are expected to be the same standard object, you don't need this try
//                      catch. Since the 100ms sample app is used for meetings with both string
//                      metadata like "someCustomerDetail" and {"isHandRaised":true} we need
//                      to be able to handle both cases without failing.
//                      Pick either json conversion or string as fits your use case and don't
//                      catch the syntax exception.
                    null
                }
            }
        }
    }

}