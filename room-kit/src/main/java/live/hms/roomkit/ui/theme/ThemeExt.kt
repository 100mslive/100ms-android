package live.hms.roomkit.ui.theme

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPreviewBinding

//get theme detail from theme utils parse it accordingly

object DefaultTheme {
    fun getColours() = ThemeColours()
}

internal fun CardView.setBackgroundAndColor(
    backgroundColorStr: String,
    @ColorRes defaultBackgroundColor: Int,
) {
    this.setCardBackgroundColor(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
}

internal fun ViewGroup.setBackgroundAndColor(
    backgroundColorStr: String,
    @ColorRes defaultBackgroundColor: Int,
) {
    setBackgroundColor(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
}

internal fun View.setBackgroundAndColor(
    backgroundColorStr: String,
    @ColorRes defaultBackgroundColor: Int,
    @DrawableRes backGroundDrawableRes: Int
) {
    this.backgroundTintList =
        ColorStateList.valueOf(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
    val normalDrawable: Drawable = this.context.resources.getDrawable(backGroundDrawableRes)
    val wrapDrawable: Drawable = DrawableCompat.wrap(normalDrawable)
    DrawableCompat.setTint(
        wrapDrawable, getColorOrDefault(backgroundColorStr, defaultBackgroundColor)
    )
    background = wrapDrawable
}

fun getColorOrDefault(colorStr: String, @ColorRes defaultColor: Int): Int {
    return try {
        colorStr.toColorInt()
    } catch (e: Exception) {
        defaultColor
    }
}

//hex color to int color
private fun String.toColorInt(): Int = android.graphics.Color.parseColor(this)


internal fun FragmentPreviewBinding.applyTheme() {
    previewCard.setBackgroundAndColor(
        DefaultTheme.getColours().surface_default,
        R.color.primary_bg
    )

    buttonNetworkQuality.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.primary_bg,
    )

    recordingView.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.primary_bg
    )


    nameTv.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp,
            R.color.primary_text
        )
    )

    descriptionTv.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_med_emp,
            R.color.muted_text
        )
    )

    recordingText.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp,
            R.color.primary_text
        )
    )

    nameInitials.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.primary_bg,
        R.drawable.circle_secondary_80
    )

    nameInitials.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp,
            R.color.primary_text
        )
    )

    iconOutputDeviceBg.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.gray_light
    )

    iconParticipantsBg.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.gray_light
    )

    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp,
            R.color.primary_text
        )
    )

    enterMeetingParentView.setBackgroundAndColor(
        DefaultTheme.getColours().primary_default,
        R.color.primary_blue,
        R.drawable.primary_blue_round_drawable
    )

}