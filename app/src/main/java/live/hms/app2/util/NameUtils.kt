package live.hms.app2.util

import java.util.*

private class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
  private val values = mutableMapOf<T, R>()
  override fun invoke(x: T): R {
    return values.getOrPut(x, { f(x) })
  }
}

fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize1(this)

object NameUtils {
  private val findWordsRegex = """\W+""".toRegex()
  val getInitials = { _name : String -> privateGetInitials(_name) }.memoize()

  private fun privateGetInitials(name: String) : String {
    val upperCased = name.trim().uppercase(Locale.ROOT)
    val words = findWordsRegex.split(upperCased)

    return when (words.size) {
        1 -> {
          if(words[0].isEmpty()) {
            "--"
          } else words[0].take(2)
        }
        else -> {
          "${words[0][0]}${words[1][0]}"
        }
    }
  }
}