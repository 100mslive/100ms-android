package live.hms.android100ms.util

object NameUtils {
  fun getInitials(name: String): String {
    val words = name.trim().split(' ')
    if (words.isEmpty()) {
      return "--"
    } else if (words.size == 1) {
      return words[0].substring(0, words[0].length.coerceAtMost(2))
    } else {
      return "${words[0][0]}${words[1][0]}"
    }
  }
}