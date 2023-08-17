package live.hms.roomkit.ui.theme

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetAudioSwitchBinding
import live.hms.roomkit.databinding.EndSessionBottomSheetBinding
import live.hms.roomkit.databinding.ExitBottomSheetBinding
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.databinding.FragmentGridVideoBinding
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.databinding.FragmentPreviewBinding
import live.hms.roomkit.databinding.VideoCardBinding
import live.hms.roomkit.drawableStart
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
            "{\"alert_error_bright\":\"#FFB2B6\",\"alert_error_brighter\":\"#FFEDEC\",\"alert_error_default\":\"#C74E5B\",\"alert_error_dim\":\"#270005\",\"alert_success\":\"#36B37E\",\"alert_warning\":\"#FFAB00\",\"background_default\":\"#0B0E15\",\"background_dim\":\"#000000\",\"border_bright\":\"#272A31\",\"border_default\":\"#1D1F27\",\"on_primary_high\":\"#FFFFFF\",\"on_primary_low\":\"#84AAFF\",\"on_primary_medium\":\"#CCDAFF\",\"on_secondary_high\":\"#FFFFFF\",\"on_secondary_low\":\"#A4ABC0\",\"on_secondary_medium\":\"#D3D9F0\",\"on_surface_high\":\"#EFF0FA\",\"on_surface_low\":\"#8F9099\",\"on_surface_medium\":\"#404759\",\"primary_bright\":\"#538dff\",\"primary_default\":\"#2572ed\",\"primary_dim\":\"#004299\",\"primary_disabled\":\"#444954\",\"secondary_bright\":\"#70778B\",\"secondary_default\":\"#444954\",\"secondary_dim\":\"#293042\",\"secondary_disabled\":\"#404759\",\"surface_bright\":\"#272A31\",\"surface_brighter\":\"#2E3038\",\"surface_default\":\"#191B23\",\"surface_dim\":\"#11131A\"}"

        return gson.fromJson(
            jsonStr, HMSRoomLayout.HMSRoomLayoutData.HMSRoomTheme.HMSColorPalette::class.java
        )
    }
}

internal fun HMSRoomLayout.getPreviewLayout() =
    this.data?.getOrNull(0)?.screens?.preview


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

internal fun ImageView.setIconEnabled(
    @DrawableRes enabledIconDrawableRes: Int
) {
    this.setBackgroundResource(R.drawable.gray_round_stroked_drawable)
    this.setImageResource(enabledIconDrawableRes)


    background.setColorFilter(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.secondaryBright,
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

    this.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
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

    this.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )

}


//hex color to int color
private fun String.toColorInt(): Int = android.graphics.Color.parseColor(this)

internal fun FragmentMeetingBinding.applyTheme() {

    buttonEndCall.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.gray_round_stroked_drawable
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

    progressBar.containerCardProgressBar.setBackgroundColor(
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

    liveTitle?.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

    tvRecordingTime?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    tvViewersCount?.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )


    //init should be called once
    buttonRaiseHand?.setIconEnabled(R.drawable.ic_raise_hand)

    (buttonOpenChat as? AppCompatImageButton)?.setIconEnabled(R.drawable.ic_chat_message)

    buttonSettingsMenu?.setIconEnabled(R.drawable.ic_settings_btn)

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

    buttonSwitchCamera?.setIconEnabled(R.drawable.ic_switch_camera)


/*    buttonSettingsMenuTop?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonSettingsMenuTop?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )*/

    //bottom menu
    bottomControls.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default,
    )

    (buttonToggleVideo as? AppCompatImageButton)?.setIconDisabled(R.drawable.ic_camera_toggle_off)

    (buttonToggleAudio as? AppCompatImageButton)?.setIconDisabled(R.drawable.ic_audio_toggle_off)




/*    buttonSettingsMenu?.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
    )

    buttonSettingsMenu?.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.borderBright,
        HMSPrebuiltTheme.getDefaults().border_bright,
        R.drawable.gray_round_stroked_drawable
    )*/


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

    containerName.setCardBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
        )
    )
    containerName.alpha = 0.64f

    name.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    iconAudioOff.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    raisedHand.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    isBrb.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
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

    closeBtn.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default,
        R.drawable.circle_secondary_40
    )

    closeBtn.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
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

    buttonNetworkQuality.setBackgroundColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    videoContainerBackground.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    previewBottomBar.setBackgroundColor(
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

    iconParticipants.setBackgroundColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default,
    )

    hlsSession.setBackgroundColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
    )

    participantCountText.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    hlsSessionText.setTextColor(
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

fun ExitBottomSheetBinding.applyTheme() {
    endSessionRoot.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    leaveTitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    leaveDescription.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceLow,
            HMSPrebuiltTheme.getDefaults().onsurface_low_emp
        )
    )

    iconEndSession.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default
    )

    endSessionTitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

    endSessionDescription.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorBright,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

    iconEndSession.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
        HMSPrebuiltTheme.getDefaults().error_default
    )

    endSessionLayout.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDim,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )
}

fun EndSessionBottomSheetBinding.applyTheme() {
    root.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    endSessionIcon.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default
    )

    endSessionTitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

    endSessionDescription.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    endSessionButton.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default,
        R.drawable.primary_round_drawable
    )

    endSessionButton.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )
}

fun FragmentGridVideoBinding.applyTheme() {
    nameInitials.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_on_to_off)
    minimizedIconAudioOff.isEnabled = false
    minimizedIconVideoOff.setIconDisabled(R.drawable.avd_video_on_to_off)
    minimizedIconVideoOff.isEnabled = false
    maximizedIcon.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    iconAudioOff.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    iconBrb.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )


    insetPill.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    youText.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    insetPillMaximised.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceDefault,
        HMSPrebuiltTheme.getDefaults().surface_default
    )

    nameInitials.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_80
    )

    tabLayoutDots.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )
    tabLayoutDots.alpha = 0.64f

/*    gridViewLinearLayout.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )
    gridViewLinearLayout.alpha = 0.64f*/

}