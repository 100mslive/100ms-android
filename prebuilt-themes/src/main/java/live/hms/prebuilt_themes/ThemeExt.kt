package live.hms.prebuilt_themes


import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.shape.CornerFamily
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import live.hms.video.signal.init.HMSRoomLayout
import live.hms.video.utils.GsonUtils.gson

//get theme detail from theme utils parse it accordingly

object HMSPrebuiltTheme {
    var theme: HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette? = null
    fun getColours() = theme
    internal fun setTheme(theme: HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette) {
        this.theme = theme
    }

    fun getDefaults() = DefaultDarkThemeColours()

    //temp
    fun getDefaultHmsColorPalette(): HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette {
        val jsonStr =
            "{\"alert_error_bright\":\"#FFB2B6\",\"alert_error_brighter\":\"#FFEDEC\",\"alert_error_default\":\"#C74E5B\",\"alert_error_dim\":\"#270005\",\"alert_success\":\"#36B37E\",\"alert_warning\":\"#FFAB00\",\"background_default\":\"#0B0E15\",\"background_dim\":\"#000000\",\"border_bright\":\"#272A31\",\"border_default\":\"#1D1F27\",\"on_primary_high\":\"#ffffff\",\"on_primary_low\":\"#7faaff\",\"on_primary_medium\":\"#cbdaff\",\"on_secondary_high\":\"#FFFFFF\",\"on_secondary_low\":\"#A4ABC0\",\"on_secondary_medium\":\"#D3D9F0\",\"on_surface_high\":\"#EFF0FA\",\"on_surface_low\":\"#8F9099\",\"on_surface_medium\":\"#C5C6D0\",\"primary_bright\":\"#3da6ff\",\"primary_default\":\"#2F80FF\",\"primary_dim\":\"#2059b2\",\"primary_disabled\":\"#338cff\",\"secondary_bright\":\"#70778B\",\"secondary_default\":\"#444954\",\"secondary_dim\":\"#293042\",\"secondary_disabled\":\"#404759\",\"surface_bright\":\"#272A31\",\"surface_brighter\":\"#2E3038\",\"surface_default\":\"#191B23\",\"surface_dim\":\"#11131A\"}"

        return gson.fromJson(
            jsonStr, HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette::class.java
        )
    }
}

internal fun HMSRoomLayout.getPreviewLayout(roleName: String?): HMSRoomLayout.HMSRoomLayoutData.Screens.Preview? {
    return if (roleName.isNullOrEmpty()) this.data?.getOrNull(0)?.screens?.preview
    else this.data?.find { it?.role == roleName }?.screens?.preview
}

internal fun HMSRoomLayout.getCurrentRoleData(roleName: String?): HMSRoomLayout.HMSRoomLayoutData? {
    return if (roleName.isNullOrEmpty()) this.data?.getOrNull(0)
    else this.data?.find { it?.role == roleName }
}


internal fun CardView.setBackgroundColor(
    backgroundColorStr: String?,
    defaultBackgroundColor: String,
) {
    this.setCardBackgroundColor(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
}

internal fun View.setBackgroundAndColor(
    backgroundColorStr: String?,
    defaultBackgroundColor: String,
) {
    setBackgroundColor(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
}


//AppCompatImageView tint
internal fun androidx.appcompat.widget.AppCompatImageView.setIconTintColor(
    iconTintColorStr: String?,
    defaultIconTintColor: String,
) {
    this.imageTintList =
        ColorStateList.valueOf(getColorOrDefault(iconTintColorStr, defaultIconTintColor))
}

internal fun View.setBackgroundAndColor(
    backgroundColorStr: String?,
    defaultBackgroundColor: String,
    @DrawableRes backGroundDrawableRes: Int?
) {
    this.backgroundTintList =
        ColorStateList.valueOf(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
    val normalDrawable: Drawable = if (backGroundDrawableRes != null) ResourcesCompat.getDrawable(
        this.context.resources,
        backGroundDrawableRes,
        null
    )!!
    else getShape()
    val wrapDrawable: Drawable = DrawableCompat.wrap(normalDrawable)
    DrawableCompat.setTint(
        wrapDrawable, getColorOrDefault(backgroundColorStr, defaultBackgroundColor)
    )
    background = wrapDrawable
}

fun getColorOrDefault(colorStr: String?, defaultColor: String): Int {
    return try {
        colorStr!!.toColorInt()
    } catch (e: Exception) {
        try {
            defaultColor.toColorInt()
        } catch (e: Exception) {
            Color.parseColor("#FFFFFF")
        }
    }
}

fun View.backgroundGradientDrawable(@ColorInt startColor: Int, @ColorInt endColor: Int): Unit {
    val h = this.height.toFloat()
    val shapeDrawable = ShapeDrawable(RectShape())
    shapeDrawable.paint.shader =
        LinearGradient(0f, 0f, 0f, h, startColor, endColor, Shader.TileMode.REPEAT)
    this.background = shapeDrawable
}


internal fun ShapeableImageView.setIconEnabled(
    @DrawableRes enabledIconDrawableRes: Int
) {
    val radius = resources.getDimension(R.dimen.eight_dp).toInt()

    this.setBackgroundColor(resources.getColor(android.R.color.transparent))

    shapeAppearanceModel =
        shapeAppearanceModel.toBuilder().setAllCorners(CornerFamily.ROUNDED, radius.toFloat())
            .build()

    this.strokeColor = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderBright,
            HMSPrebuiltTheme.getDefaults().border_bright
        )
    )

    this.strokeWidth = resources.getDimension(R.dimen.one_dp)

    this.setImageResource(enabledIconDrawableRes)

    drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    (drawable as? Animatable)?.start()
}


internal fun ImageView.setIconDisabled(
    @DrawableRes disabledIconDrawableRes: Int,
    @DrawableRes backgroundRes: Int = R.drawable.gray_round_solid_drawable
) {

    this.setImageResource(disabledIconDrawableRes)
    this.setBackgroundResource(backgroundRes)
    background.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.secondaryDim,
            HMSPrebuiltTheme.getDefaults().secondary_dim
        )
    )

    drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    (drawable as? Animatable)?.start()

}

internal fun ShapeableImageView.setIconDisabled(
    @DrawableRes disabledIconDrawableRes: Int,
    @DimenRes radiusREs: Int = R.dimen.eight_dp,
) {

    val radius = resources.getDimension(radiusREs).toInt()

    this.strokeWidth = 0f

    shapeAppearanceModel =
        shapeAppearanceModel.toBuilder().setAllCornerSizes(radius.toFloat()).build()

    this.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.secondaryDim,
            HMSPrebuiltTheme.getDefaults().secondary_dim
        )
    )
    this.setImageResource(disabledIconDrawableRes)


    drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    (drawable as? Animatable)?.start()

}

internal fun TextView.alertButtonEnabled() {
    this.isEnabled = true

    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        null
    )

    this.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )
}

internal fun TextView.buttonStrokeEnabled() {
    this.isEnabled = true
    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.gray_round_stroked_drawable
    )

    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

}

internal fun TextView.buttonEnabled() {
    this.isEnabled = true

    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        null
    )

    this.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )
}

private fun getBackgroundForColor(color: Int) = getShape().apply { setTint(color) }

internal fun TextView.buttonDisabled() {
    this.isEnabled = false


    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )

    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDisabled,
        HMSPrebuiltTheme.getDefaults().primary_disabled,
        null
    )

    this.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )

}


//hex color to int color
private fun String.toColorInt(): Int = Color.parseColor(this)

fun pinMessageTheme(pinCloseButton: ImageView) {
    pinCloseButton.drawable?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

}

private fun chatPausedTheme(
    chatPausedContainer: LinearLayoutCompat, chatPausedTitle: TextView, chatPausedBy: TextView
) {
    chatPausedContainer.background = getChatBackgroundDrawable()

    chatPausedTitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )
    chatPausedBy.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )
}

private fun userBlockedTheme(userBlocked: TextView) {
    userBlocked.background = getChatBackgroundDrawable()
    userBlocked.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )
}

private fun closeButtonTheme(closeCombinedTabButton: AppCompatImageButton, res: Resources) {
    closeCombinedTabButton.setBackgroundDrawable(ResourcesCompat.getDrawable(
        res, R.drawable.ic_cross, null
    )?.apply {
        setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                HMSPrebuiltTheme.getDefaults().onsurface_med_emp
            )
        )
    })
}

fun getShape(radiusAt: ApplyRadiusatVertex = ApplyRadiusatVertex.ALL_CORNERS): ShapeDrawable {
    val eightDp = 8.dp().toFloat()

    val lines = floatArrayOf(
        if (radiusAt == ApplyRadiusatVertex.TOP || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.TOP || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.TOP || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.TOP || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.BOTTOM || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.BOTTOM || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.BOTTOM || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f,
        if (radiusAt == ApplyRadiusatVertex.BOTTOM || radiusAt == ApplyRadiusatVertex.ALL_CORNERS) eightDp else 0f
    )

    return ShapeDrawable(
        RoundRectShape(
            lines, null, null
        )
    )
}

fun getChatBackgroundDrawable(alpha: Double? = null): ShapeDrawable {
    return getShape()//ResourcesCompat.getDrawable(this.root.resources,R.drawable.send_message_background, null)!!
        .apply {
            val initialColor = if (alpha == null) HMSPrebuiltTheme.getColours()?.surfaceDefault
            else HMSPrebuiltTheme.getColours()?.surfaceDefault?.let { addAlpha(it, alpha) }
            val defaultColor: String =
                if (alpha == null) HMSPrebuiltTheme.getDefaults().surface_default
                else addAlpha(HMSPrebuiltTheme.getDefaults().surface_default, alpha)
            val color = getColorOrDefault(
                initialColor, defaultColor
            )
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                color,
                BlendModeCompat.SRC
            )
        }
}

private fun configureChatControlsTheme(
    sendToBackground: MaterialCardView,
    sendToChipText: MaterialTextView,
    chatOptionsCard: MaterialCardView,
    chatOptions: ImageView
) {
    chatOptions.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceLow,
            HMSPrebuiltTheme.getDefaults().onsurface_low_emp
        )
    )

    chatOptionsCard.strokeColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.borderBright, HMSPrebuiltTheme.getDefaults().border_bright
    )
    chatOptionsCard.strokeWidth = 1.dp()
    chatOptionsCard.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.surfaceDefault,
            HMSPrebuiltTheme.getDefaults().surface_default
        )
    )

    sendToBackground.background = getShape().apply {
        val color = getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
        colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
    }

    sendToChipText.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )
    sendToBackground.strokeWidth = 0
//    sendToBackground.strokeColor = getColorOrDefault(
//        HMSPrebuiltTheme.getColours()?.borderBright,
//        HMSPrebuiltTheme.getDefaults().border_bright
//    )

    sendToChipText.drawableEnd?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )
}

private fun TextInputLayout.applyTheme() {
    // text color
    // hint color
    // background color

    background = getChatBackgroundDrawable()
    defaultHintTextColor = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceLow,
            HMSPrebuiltTheme.getDefaults().onsurface_low_emp
        )
    )
    startIconDrawable =
        ResourcesCompat.getDrawable(resources, R.drawable.ic_search_24, null)?.apply {
                colorFilter = PorterDuffColorFilter(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                        HMSPrebuiltTheme.getDefaults().onsurface_low_emp
                    ), PorterDuff.Mode.SRC_IN
                )
            }
//    boxStrokeColor = getColorOrDefault(
//        HMSPrebuiltTheme.getColours()?.borderBright,
//        HMSPrebuiltTheme.getDefaults().border_bright
//    )
    boxStrokeWidth = 0
    boxStrokeWidthFocused = 0
}

private fun TextInputEditText.applyTheme() {
    setHintTextColor(
        ColorStateList.valueOf(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                HMSPrebuiltTheme.getDefaults().onsurface_low_emp
            )
        )
    )
    setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )
    background = getChatBackgroundDrawable()
}

private fun backgroundShape(
    inset: Boolean = false,
    innerRadii: Float = 8.dp().toFloat()
): ShapeDrawable {
    val lines = floatArrayOf(
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii,
        innerRadii
    )
    return if (inset) {
        ShapeDrawable(
            RoundRectShape(
                lines, RectF(1f, 1f, 1f, 1f), lines
            )
        )
    } else {
        ShapeDrawable(
            RoundRectShape(
                lines, null, null
            )
        )
    }
}

fun trackTintList(): ColorStateList {
    val checkedUncheckedState = arrayOf(
        intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)
    )

    return ColorStateList(
        checkedUncheckedState, intArrayOf(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.primaryDefault,
                HMSPrebuiltTheme.getDefaults().primary_default
            ), getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                HMSPrebuiltTheme.getDefaults().onsurface_med_emp
            )
        )
    )
}

private fun setSwitchThemes(switchCompat: SwitchMaterial) {
    with(switchCompat) {
        thumbTintList = thumbTintList()
        trackTintList = trackTintList()
    }

    switchCompat.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

}

private fun thumbTintList(): ColorStateList {
    val checkedUncheckedState = arrayOf(
        intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)
    )

    return ColorStateList(
        checkedUncheckedState, intArrayOf(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
                HMSPrebuiltTheme.getDefaults().onprimary_high_emp
            ), getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.secondaryDefault,
                HMSPrebuiltTheme.getDefaults().secondary_default
            )
        )
    )
}

// Polls

fun Button.voteButtons() {

    val buttonDisabledBackgroundColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.primaryDisabled,
        HMSPrebuiltTheme.getDefaults().primary_disabled
    )
    val buttonDisabledTextColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onPrimaryLow,
        HMSPrebuiltTheme.getDefaults().onprimary_low_emp
    )


    val buttonEnabledBackgroundColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default
    )

    val buttonEnabledTextColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
        HMSPrebuiltTheme.getDefaults().onprimary_high_emp
    )

    val states = arrayOf(
        intArrayOf(android.R.attr.state_enabled), intArrayOf(-android.R.attr.state_enabled)
    )
    val backgroundColors = intArrayOf(buttonEnabledBackgroundColor, buttonDisabledBackgroundColor)
    val textColors = intArrayOf(buttonEnabledTextColor, buttonDisabledTextColor)

    backgroundTintList = ColorStateList(
        states, backgroundColors
    )

    setTextColor(
        ColorStateList(
            states, textColors
        )
    )

}


fun MaterialCardView.isSelectedStroke(isSelected: Boolean) {
    if (isSelected.not()) this.strokeColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.borderBright, HMSPrebuiltTheme.getDefaults().primary_default
    )
    else this.strokeColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default
    )

}


internal fun TextView.saveButtonEnabled() {
    this.isEnabled = true

    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        null
    )

}


internal fun TextView.saveButtonDisabled() {
    this.isEnabled = false


    this.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )

    this.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDim,
        HMSPrebuiltTheme.getDefaults().primary_disabled,
        null
    )


}

fun LinearProgressIndicator.applyProgressbarTheme() {
    trackColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.surfaceBright, HMSPrebuiltTheme.getDefaults().primary_default
    )

    setIndicatorColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().surface_bright
        )
    )

}

fun MaterialCardView.highlightCorrectAnswer(isCorrect: Boolean) {
    strokeColor = if (isCorrect) {
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertSuccess,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    } else {
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    }
    strokeWidth = 1.dp()
}

private fun dialogBackground(resources: Resources): Drawable =
    ResourcesCompat.getDrawable(resources, R.drawable.gray_shape_round_dialog, null)!!.apply {
        val color = getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().background_default
        )
        setColorFilter(color, PorterDuff.Mode.ADD)
    }

private fun searchViewTheme(containerSearch: MaterialCardView, textInputSearch: EditText) {

    textInputSearch.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    textInputSearch.setHintTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )
    textInputSearch.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.surfaceDim, HMSPrebuiltTheme.getDefaults().surface_dim
        )
    )
    textInputSearch.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )
    containerSearch.strokeWidth = 1.dp()
    containerSearch.strokeColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.borderBright, HMSPrebuiltTheme.getDefaults().border_bright
    )
}