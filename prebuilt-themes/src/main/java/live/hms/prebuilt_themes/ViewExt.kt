package live.hms.prebuilt_themes

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.TextView
import kotlin.math.roundToInt

enum class ApplyRadiusatVertex {
    TOP,
    ALL_CORNERS,
    BOTTOM,
    NONE
}
private fun getDip(): Float = Resources.getSystem().displayMetrics.density
fun Float.dp() = this * getDip()
fun Int.dp() = (this * getDip()).roundToInt()
fun addAlpha(originalColor: String, alpha: Double): String {

    val alphaFixed = (alpha * 255)
    var alphaHex = java.lang.Double.toHexString(alphaFixed)
    if (alphaHex.length == 1) {
        alphaHex = "0$alphaHex"
    }
    return originalColor.replace("#", "#$alphaHex")
}

var TextView.drawableStart: Drawable?
    get() = drawables[0]
    set(value) = setDrawables(value, drawableTop, drawableEnd, drawableBottom)

var TextView.drawableTop: Drawable?
    get() = drawables[1]
    set(value) = setDrawables(drawableStart, value, drawableEnd, drawableBottom)

var TextView.drawableEnd: Drawable?
    get() = drawables[2]
    set(value) = setDrawables(drawableStart, drawableTop, value, drawableBottom)

var TextView.drawableBottom: Drawable?
    get() = drawables[3]
    set(value) = setDrawables(drawableStart, drawableTop, drawableEnd, value)

@Deprecated("Consider replace with drawableStart to better support right-to-left Layout", ReplaceWith("drawableStart"))
var TextView.drawableLeft: Drawable?
    get() = compoundDrawables[0]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(value, drawableTop, drawableRight, drawableBottom)

@Deprecated("Consider replace with drawableEnd to better support right-to-left Layout", ReplaceWith("drawableEnd"))
var TextView.drawableRight: Drawable?
    get() = compoundDrawables[2]
    set(value) = setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, value, drawableBottom)

private val TextView.drawables: Array<Drawable?>
    get() = if (Build.VERSION.SDK_INT >= 17) compoundDrawablesRelative else compoundDrawables

fun TextView.setDrawables(start: Drawable? = drawableStart, top: Drawable? = drawableTop, end: Drawable? = drawableEnd, bottom: Drawable? = drawableBottom) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)
}
