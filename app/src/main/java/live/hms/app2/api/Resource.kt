package live.hms.app2.api


data class Resource<out T>(val status: Status, val data: T?, val message: String?) {
  companion object {
    fun <T> success(data: T): Resource<T> =
      Resource(status = Status.SUCCESS, data = data, message = null)

    fun <T> error(message: String? = null): Resource<T> =
      Resource(status = Status.ERROR, data = null, message = message)

    fun <T> loading(): Resource<T> =
      Resource(status = Status.LOADING, data = null, message = null)
  }
}