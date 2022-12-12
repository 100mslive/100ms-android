package live.hms.app2.model

data class MetaDataModel(
    val payload: String,
    val duration: Long,
    val metaData: String,
){
    var startTime: Long = 0
}