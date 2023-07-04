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
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.databinding.VideoCardBinding
import live.hms.video.signal.init.LayoutResult

//get theme detail from theme utils parse it accordingly

object HMSPrebuiltTheme {
    var theme: LayoutResult.Data.Theme.Palette? = null
    fun getColours() = theme
    internal fun setTheme(theme: LayoutResult.Data.Theme.Palette) {
        this.theme = theme
    }

    fun getDefaults() = ThemeColours()
}

internal fun CardView.setBackgroundAndColor(
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
    this.imageTintList = ColorStateList.valueOf(getColorOrDefault(iconTintColorStr, defaultIconTintColor))
}

internal fun View.setBackgroundAndColor(
    backgroundColorStr: String?,
    defaultBackgroundColor: String,
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

fun getColorOrDefault(colorStr: String?,defaultColor: String): Int {
    return try {
        colorStr!!.toColorInt()
    } catch (e: Exception) {
        try {
            defaultColor.toColorInt()
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#FFFFFF")
        }
    }
}

//hex color to int color
private fun String.toColorInt(): Int = android.graphics.Color.parseColor(this)

internal fun FragmentMeetingBinding.applyTheme() {

    buttonEndCall.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.ic_icon_end_call
    )


    progressBar.containerCardProgressBar.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    progressBar.heading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    progressBar.description.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh, HMSPrebuiltTheme.getDefaults().onsecondary_high_emp
        )
    )

//    progressBar.progressBarX.progressTintList = ColorStateList.valueOf(
//        getColorOrDefault(
//            DefaultTheme.getColours().onsecondary_med_emp, R.color.muted_text
//        )
//    )


    topMenu?.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim, HMSPrebuiltTheme.getDefaults().background_default
        )
    )

    liveTitle?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh, HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    tvRecordingTime?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium, HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    tvViewersCount?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium, HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )


    //init should be called once
    buttonRaiseHand?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )
    buttonRaiseHand?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    (buttonOpenChat as? AppCompatImageView)?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonOpenChat.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )



    unreadMessageCount.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default,
        R.drawable.badge_circle_20
    )

    unreadMessageCount.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,  HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )


    buttonSettingsMenuTop?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonSettingsMenuTop?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )

    //bottom menu
    bottomControls.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default,
    )

    (buttonToggleVideo as? AppCompatImageView)?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonToggleVideo.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )

    (buttonToggleAudio as? AppCompatImageView)?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonToggleAudio.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )

    buttonSettingsMenu?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonSettingsMenu?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )


}

internal fun FragmentActiveSpeakerBinding.applyTheme() {
    root?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )
}

internal fun VideoCardBinding.applyTheme() {
    nameInitials.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,  HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )
}

internal fun FragmentPreviewBinding.applyTheme() {
    previewCard.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    buttonNetworkQuality.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    recordingView.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,  HMSPrebuiltTheme.getDefaults().secondary_default
    )


    nameTv.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,  HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    descriptionTv.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryMedium,  HMSPrebuiltTheme.getDefaults().onprimary_med_emp
        )
    )

    recordingText.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryMedium, HMSPrebuiltTheme.getDefaults().onprimary_med_emp
        )
    )

    nameInitials.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,  HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_80
    )

    nameInitials.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,  HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    iconOutputDeviceBg.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,  HMSPrebuiltTheme.getDefaults().secondary_default
    )

    iconParticipantsBg.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,  HMSPrebuiltTheme.getDefaults().secondary_default
    )

    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,  HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    enterMeetingParentView.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
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