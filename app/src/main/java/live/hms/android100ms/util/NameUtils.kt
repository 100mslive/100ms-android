package live.hms.android100ms.util

object NameUtils {
  fun getInitials(name: String): String {
    val characters = name
      .replace(Regex("[^a-z ]"), "")

    return if (characters.isEmpty()) {
      "--"
    } else {
      characters.split(' ')
        .mapNotNull { it.firstOrNull()?.toString() }
        .reduce { acc, s -> acc + s }
    }
  }
}