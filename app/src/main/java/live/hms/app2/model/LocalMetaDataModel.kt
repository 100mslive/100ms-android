package live.hms.app2.model

data class LocalMetaDataModel(
    val payload: String,
    val duration: Long
){
    var startTime: Long = 0
}