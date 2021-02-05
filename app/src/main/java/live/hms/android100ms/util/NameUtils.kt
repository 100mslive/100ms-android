package live.hms.android100ms.util

object NameUtils {
  fun getInitials(name: String): String {
    return if (name.isEmpty()) {
      "--"
    } else {
      name.split(' ')
        .mapNotNull { it.firstOrNull()?.toString() }
        .reduce { acc, s -> acc + s }
    }
  }
}