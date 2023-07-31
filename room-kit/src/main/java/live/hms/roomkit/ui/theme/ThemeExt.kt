package live.hms.roomkit.ui.theme

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.button.MaterialButton
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.databinding.FragmentPermissionBinding
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.databinding.VideoCardBinding
import live.hms.video.signal.init.HMSRoomLayout
import live.hms.video.utils.GsonUtils.gson

//get theme detail from theme utils parse it accordingly

object HMSPrebuiltTheme {
    var theme: HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette? = null
    fun getColours() = theme
    internal fun setTheme(theme: HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette) {
        this.theme = theme

        this.theme = theme.copy(
            backgroundDim = "#000000",
            primaryDefault = "#2572ED",
            borderBright = "#272A31",
            onSurfaceHigh = "#EFF0FA",
            secondaryDim = "#293042",
            backgroundDefault = "#0B0E15",
            onPrimaryLow = "#84AAFF",
            primaryDisabled = "#004299"
        )
    }

    fun getDefaults() = DefaultDarkThemeColours()

    //temp
    fun getDefaultHmsColorPalette(): HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette {
        val jsonStr =
            "{\"alert_error_bright\":\"#FFB2B6\",\"alert_error_brighter\":\"#FFEDEC\",\"alert_error_default\":\"#C74E5B\",\"alert_error_dim\":\"#270005\",\"alert_success\":\"#36B37E\",\"alert_warning\":\"#FFAB00\",\"background_default\":\"#0B0E15\",\"background_dim\":\"#000000\",\"border_bright\":\"#272A31\",\"border_default\":\"#1D1F27\",\"on_primary_high\":\"#FFFFFF\",\"on_primary_low\":\"#84AAFF\",\"on_primary_medium\":\"#CCDAFF\",\"on_secondary_high\":\"#FFFFFF\",\"on_secondary_low\":\"#A4ABC0\",\"on_secondary_medium\":\"#D3D9F0\",\"on_surface_high\":\"#EFF0FA\",\"on_surface_low\":\"#8F9099\",\"on_surface_medium\":\"#404759\",\"primary_bright\":\"#538dff\",\"primary_default\":\"#2572ed\",\"primary_dim\":\"#004299\",\"primary_disabled\":\"#444954\",\"secondary_bright\":\"#70778B\",\"secondary_default\":\"#444954\",\"secondary_dim\":\"#293042\",\"secondary_disabled\":\"#404759\",\"surface_bright\":\"#272A31\",\"surface_brighter\":\"#2E3038\",\"surface_default\":\"#191B23\",\"surface_dim\":\"#11131A\"}"

        return gson.fromJson(
            jsonStr, HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette::class.java
        )
    }
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
    this.imageTintList =
        ColorStateList.valueOf(getColorOrDefault(iconTintColorStr, defaultIconTintColor))
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

fun getColorOrDefault(colorStr: String?, defaultColor: String): Int {
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

internal fun ImageButton.setIconEnabled(
    @DrawableRes disabledIconDrawableRes: Int
) {
    this.setBackgroundResource(R.drawable.gray_round_stroked_drawable)
    this.setImageResource(disabledIconDrawableRes)


    background.setColorFilter(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderBright,
            HMSPrebuiltTheme.getDefaults().border_bright
        ), PorterDuff.Mode.DST_OVER
    )

    drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    (drawable as? Animatable)?.start()
}


internal fun ImageButton.setIconDisabled(
    @DrawableRes disabledIconDrawableRes: Int
) {
    this.setImageResource(disabledIconDrawableRes)
    this.setBackgroundResource(R.drawable.gray_round_solid_drawable)
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
        R.drawable.blue_round_solid_drawable
    )
}

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
        R.drawable.blue_round_solid_drawable
    )
}


//hex color to int color
private fun String.toColorInt(): Int = android.graphics.Color.parseColor(this)

internal fun FragmentMeetingBinding.applyTheme() {

    buttonEndCall.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.ic_icon_end_call
    )

    meetingFragmentProgress?.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
        )
    )

    meetingFragmentProgressBar?.progressTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
    )


    progressBar.containerCardProgressBar.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    progressBar.heading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    progressBar.description.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onsecondary_high_emp
        )
    )

//    progressBar.progressBarX.progressTintList = ColorStateList.valueOf(
//        getColorOrDefault(
//            DefaultTheme.getColours().onsecondary_med_emp, R.color.muted_text
//        )
//    )


    topMenu?.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
        )
    )

    liveTitle?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    tvRecordingTime?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    tvViewersCount?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )


    //init should be called once
    buttonRaiseHand?.setIconDisabled(R.drawable.ic_raise_hand)

    (buttonOpenChat as? AppCompatImageButton)?.setIconDisabled(R.drawable.ic_chat_message)




    unreadMessageCount.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default,
        R.drawable.badge_circle_20
    )

    unreadMessageCount.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
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

    (buttonToggleVideo as? AppCompatImageButton)?.setIconDisabled(R.drawable.ic_camera_toggle_off)

    (buttonToggleAudio as? AppCompatImageButton)?.setIconDisabled(R.drawable.ic_audio_toggle_off)




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
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    nameInitials.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_80
    )


    root.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().background_default
        )
    )


}

internal fun FragmentPermissionBinding.applyTheme() {
    root.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().background_default
        )
    )

    heading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    subtitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_med_emp
        )

    )

    buttonGrantPermission.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
    )

    buttonGrantPermission.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )


}

internal fun BottomSheetAudioSwitchBinding.applyTheme() {
}

internal fun FragmentPreviewBinding.applyTheme() {

    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    buttonJoinMeeting.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        R.drawable.blue_round_solid_drawable
    )

    joinLoader.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDisabled,
        HMSPrebuiltTheme.getDefaults().primary_disabled,
        R.drawable.blue_round_solid_drawable
    )

    joinLoaderProgress.progressTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )



    editContainerName.boxStrokeColor = getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default
    )

    editContainerName.hintTextColor = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceLow,
            HMSPrebuiltTheme.getDefaults().onsurface_low_emp
        )
    )


    editTextName.isCursorVisible = true

    editTextName.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    previewCard.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim, HMSPrebuiltTheme.getDefaults().surface_default
    )

    buttonNetworkQuality.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    videoContainerBackground.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    previewBottomBar.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDefault,
        HMSPrebuiltTheme.getDefaults().background_default
    )




    nameTv.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    descriptionTv.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryMedium,
            HMSPrebuiltTheme.getDefaults().onprimary_med_emp
        )
    )

    iconParticipants.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default,
        R.drawable.gray_round_solid_drawable
    )

    hlsSession.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.gray_round_solid_drawable
    )

    iconParticipants.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    hlsSession.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )


    nameInitials.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_80
    )

    nameInitials.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onsecondary_high_emp
        )
    )


    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

//    buttonJoinMeeting.setBackgroundColor(
//        getColorOrDefault(
//            HMSPrebuiltTheme.getColours()?.primaryDefault,
//            HMSPrebuiltTheme.getDefaults().primary_default
//        )
//    )

    //only init state
    buttonToggleVideo.setIconDisabled(R.drawable.avd_video_on_to_off)
    buttonToggleAudio.setIconDisabled(R.drawable.avd_mic_on_to_off)
    buttonSwitchCamera.setIconEnabled(R.drawable.ic_switch_camera)

}