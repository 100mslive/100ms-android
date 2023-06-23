package live.hms.roomkit.ui.theme

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentMeetingBinding
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


//AppCompatImageView tint
internal fun androidx.appcompat.widget.AppCompatImageView.setIconTintColor(
    iconTintColorStr: String,
    @ColorRes defaultIconTintColor: Int,
) {
    this.imageTintList = ColorStateList.valueOf(getColorOrDefault(iconTintColorStr, defaultIconTintColor))
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

internal fun FragmentMeetingBinding.applyTheme() {


    progressBar.containerCardProgressBar.setBackgroundAndColor(
        DefaultTheme.getColours().surface_default,
        R.color.primary_bg
    )

    progressBar.heading.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsurface_high_emp, R.color.primary_text
        )
    )

    progressBar.description.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsecondary_med_emp, R.color.muted_text
        )
    )

//    progressBar.progressBarX.progressTintList = ColorStateList.valueOf(
//        getColorOrDefault(
//            DefaultTheme.getColours().onsecondary_med_emp, R.color.muted_text
//        )
//    )


    topMenu?.setBackgroundColor(
        getColorOrDefault(
            DefaultTheme.getColours().background_default, R.color.primary_bg
        )
    )

    liveTitle?.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsurface_high_emp, R.color.primary_text
        )
    )

    tvRecordingTime?.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsurface_med_emp, R.color.primary_text
        )
    )

    tvViewersCount?.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsurface_med_emp, R.color.primary_text
        )
    )


    //init should be called once
    buttonRaiseHand?.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )
    buttonRaiseHand?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    (buttonOpenChat as? AppCompatImageView)?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    buttonOpenChat.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )



    unreadMessageCount.setBackgroundAndColor(
        DefaultTheme.getColours().surface_default,
        R.color.primary_bg,
        R.drawable.badge_circle_20
    )

    unreadMessageCount.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onsurface_high_emp, R.color.primary_text
        )
    )


    buttonSettingsMenuTop?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    buttonSettingsMenuTop?.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )

    //bottom menu
    bottomControls.setBackgroundAndColor(
        DefaultTheme.getColours().background_default,
        R.color.primary_bg
    )

    (buttonToggleVideo as? AppCompatImageView)?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    buttonToggleVideo.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )

    (buttonToggleAudio as? AppCompatImageView)?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    buttonToggleAudio.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )

    buttonSettingsMenu?.setIconTintColor(
        DefaultTheme.getColours().onsurface_high_emp,
        R.color.gray_light
    )

    buttonSettingsMenu?.setBackgroundAndColor(
        DefaultTheme.getColours().border_bright,
        R.color.gray_light,
        R.drawable.gray_round_stroked_drawable
    )


}


internal fun FragmentPreviewBinding.applyTheme() {
    previewCard.setBackgroundAndColor(
        DefaultTheme.getColours().surface_default, R.color.primary_bg
    )

    buttonNetworkQuality.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.primary_bg,
    )

    recordingView.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default, R.color.primary_bg
    )


    nameTv.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp, R.color.primary_text
        )
    )

    descriptionTv.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_med_emp, R.color.muted_text
        )
    )

    recordingText.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp, R.color.primary_text
        )
    )

    nameInitials.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default,
        R.color.primary_bg,
        R.drawable.circle_secondary_80
    )

    nameInitials.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp, R.color.primary_text
        )
    )

    iconOutputDeviceBg.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default, R.color.gray_light
    )

    iconParticipantsBg.setBackgroundAndColor(
        DefaultTheme.getColours().secondary_default, R.color.gray_light
    )

    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            DefaultTheme.getColours().onprimary_high_emp, R.color.primary_text
        )
    )

    enterMeetingParentView.setBackgroundAndColor(
        DefaultTheme.getColours().primary_default,
        R.color.primary_blue,
        R.drawable.primary_blue_round_drawable
    )

//    //TODO only init state
//    buttonToggleVideoBg.setBackgroundAndColor(
//        DefaultTheme.getColours().secondary_default,
//        R.color.gray_light,
//    )
//    //TODO only init state
//    buttonToggleAudioBg.setBackgroundAndColor(
//        DefaultTheme.getColours().secondary_default,
//        R.color.gray_light,
//    )


}