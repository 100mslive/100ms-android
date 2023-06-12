package live.hms.roomkit.ui


data class HMSPrebuiltOptions(
    val userName: String? = null,
    val userId: String? = null,
    val endPoints: HashMap<String, String>? = null,
    val debugInfo: Boolean = false,
    val environment: String? = null,
) {

}