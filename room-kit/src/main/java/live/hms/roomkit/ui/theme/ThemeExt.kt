package live.hms.roomkit.ui.theme

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import live.hms.roomkit.R
import live.hms.roomkit.databinding.*
import live.hms.roomkit.drawableLeft
import live.hms.roomkit.drawableStart
import live.hms.roomkit.setGradient
import live.hms.roomkit.util.EmailUtils
import live.hms.roomkit.util.dp
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

internal fun HMSRoomLayout.getPreviewLayout(roleName : String?) : HMSRoomLayout.HMSRoomLayoutData.Screens.Preview? {
   return if (roleName.isNullOrEmpty())
        this.data?.getOrNull(0)?.screens?.preview
    else
        this.data?.find { it?.role == roleName }?.screens?.preview
}

internal fun HMSRoomLayout.getCurrentRoleData(roleName : String?) : HMSRoomLayout.HMSRoomLayoutData? {
    return if (roleName.isNullOrEmpty())
        this.data?.getOrNull(0)
    else
        this.data?.find { it?.role == roleName }
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
    @DrawableRes backGroundDrawableRes: Int
) {
    this.backgroundTintList =
        ColorStateList.valueOf(getColorOrDefault(backgroundColorStr, defaultBackgroundColor))
    val normalDrawable: Drawable = ResourcesCompat.getDrawable(this.context.resources, backGroundDrawableRes, null)!!
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
    chatView.background = getChatBackgroundDrawable()
    editTextMessage.background = getChatBackgroundDrawable()
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

    liveTitle?.setTextColor(Color.WHITE)

    liveTitleCard.backgroundTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

//    liveTitleCard?.setBackgroundColor(
//        getColorOrDefault(
//            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
//            HMSPrebuiltTheme.getDefaults().error_default
//        )
//    )

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

    tvViewersCount.drawableLeft?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    recordingSignal.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    recordingSignalProgress.progressTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

//    tvViewersCount.backgroundTintList = ColorStateList.valueOf(
//        getColorOrDefault(
//            HMSPrebuiltTheme.getColours()?.borderBright,
//            HMSPrebuiltTheme.getDefaults().surface_default
//        )
//    )




    //init should be called once
    buttonRaiseHand?.setIconEnabled(R.drawable.ic_raise_hand)

    (buttonOpenChat)?.setIconEnabled(R.drawable.ic_chat_message)

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

    (buttonToggleVideo)?.setIconDisabled(R.drawable.ic_camera_toggle_off)

    (buttonToggleAudio)?.setIconDisabled(R.drawable.ic_audio_toggle_off)


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

internal fun ChangeNameFragmentBinding.applyTheme() {
    title.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    closeBtn.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    border.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderBright,
            HMSPrebuiltTheme.getDefaults().border_bright
        )
    )


    standardBottomSheet.background =root.context.resources.getDrawable(R.drawable.gray_shape_round_dialog)
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.backgroundDim,
                HMSPrebuiltTheme.getDefaults().background_default)
            setColorFilter(color, PorterDuff.Mode.ADD);
        }

    newName.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.surfaceDefault,
            HMSPrebuiltTheme.getDefaults().surface_default
        )
    )

    newName.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    changeNameDec.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    changeName.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        R.drawable.blue_round_solid_drawable
    )

    changeName.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

}

internal fun BottomSheetStopRecordingBinding.applyTheme() {
    title.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    title.drawableStart?.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    closeBtn.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )


    standardBottomSheet.background =root.context.resources.getDrawable(R.drawable.gray_shape_round_dialog)
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceDim,
                HMSPrebuiltTheme.getDefaults().background_default)
            setColorFilter(color, PorterDuff.Mode.ADD);
        }


    changeNameDec.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        )
    )

    changeName.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        R.drawable.blue_round_solid_drawable
    )

    changeName.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
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

    iconAudioOff.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    iconMaximised.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )


    iconMaximised.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    audioLevel.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    raisedHand.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    raisedHand.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    isBrb.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    isBrb.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

}

internal fun BottomSheetAudioSwitchBinding.applyTheme() {
}

internal fun FragmentRolePreviewBinding.applyTheme() {
    buttonJoinMeeting.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    heading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    subheading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsecondary_med_emp
        )
    )



    buttonJoinMeeting.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.primaryDefault,
        HMSPrebuiltTheme.getDefaults().primary_default,
        R.drawable.blue_round_solid_drawable
    )

    previewCard.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim, HMSPrebuiltTheme.getDefaults().surface_default
    )


    videoContainerBackground.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    previewBottomBar.setBackgroundColor(
        HMSPrebuiltTheme.getColours()?.backgroundDefault,
        HMSPrebuiltTheme.getDefaults().background_default
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

    declineButton.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDefault,
        HMSPrebuiltTheme.getDefaults().background_default,
        R.drawable.blue_round_solid_drawable
    )


    declineButton.setTextColor(
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

//internal fun FragmentRolePreview.applyTheme() {
//
//}
internal fun FragmentPreviewBinding.applyTheme() {

    previewGradient.setGradient(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.backgroundDim,
        HMSPrebuiltTheme.getDefaults().background_default
    )
    , Color.TRANSPARENT)

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

    joinLoaderProgress.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        ),
        PorterDuff.Mode.SRC_IN
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

    editContainerName.defaultHintTextColor = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
    )


    editContainerName.requestFocus()

    editContainerName.hintTextColor = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
    )


    joinLoaderProgress.progressTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryLow,
            HMSPrebuiltTheme.getDefaults().onprimary_low_emp
        )
    )

    editTextName.isCursorVisible = true

    editTextName.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )
    rootLayout.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_default
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
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    descriptionTv.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
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
            HMSPrebuiltTheme.getColours()?.alertErrorBrighter,
            HMSPrebuiltTheme.getDefaults().border_bright)
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
        HMSPrebuiltTheme.getColours()?.surfaceDim, HMSPrebuiltTheme.getDefaults().background_default
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

    leaveIcon.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp
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

    root.background = ResourcesCompat.getDrawable(this.root.resources,R.drawable.gray_shape_round_dialog, null)!!
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceDim,
                HMSPrebuiltTheme.getDefaults().background_default)
            colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
        }


    endSessionIcon.setIconTintColor(
        HMSPrebuiltTheme.getColours()?.alertErrorDefault,
        HMSPrebuiltTheme.getDefaults().error_default
    )

    endSessionTitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )

    endSessionDescription.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
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
    tabLayoutDots.setBackgroundColor(Color.TRANSPARENT)
    tabLayoutDots.backgroundTintList = ColorStateList.valueOf(
            Color.TRANSPARENT
    )

    tabLayoutDotsRemoteScreenShare.setBackgroundColor(Color.TRANSPARENT)
    tabLayoutDotsRemoteScreenShare.backgroundTintList = ColorStateList.valueOf(
        Color.TRANSPARENT
    )

    tabLayoutDots.tabIconTint = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    tabLayoutDots.setSelectedTabIndicatorColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    tabLayoutDotsRemoteScreenShare.setSelectedTabIndicatorColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )

    tabLayoutDotsRemoteScreenShare.tabIconTint = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
    )





    tabLayoutDotsRemoteScreenShare.setBackgroundColor(Color.TRANSPARENT)

    iconOption.setBackgroundColor(
        getColorOrDefault(
            EmailUtils.addAlpha(HMSPrebuiltTheme.getColours()?.surfaceDefault!!, 0.6),
            HMSPrebuiltTheme.getDefaults().surface_default
        )
    )


    rootLayout.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDefault,
            HMSPrebuiltTheme.getDefaults().surface_default
        )
    )


    minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_on_to_off, radiusREs = R.dimen.two_dp)
    minimizedIconAudioOff.isEnabled = false
    minimizedIconVideoOff.setIconDisabled(R.drawable.avd_video_on_to_off, radiusREs = R.dimen.two_dp)
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

    iconAudioLevel.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )



    iconBrb.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.circle_secondary_32
    )

    iconOption.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.blue_round_solid_drawable
    )

    iconOption.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        )
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

fun ItemGridOptionBinding.applyTheme() {
    progressBar.progressTintList = ColorStateList.valueOf(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default
        )
    )

    rootLayout.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.backgroundDefault,
        HMSPrebuiltTheme.getDefaults().background_default
    )

    participantImage.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    subtitle.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    participantCountText.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.surfaceBrighter,
        HMSPrebuiltTheme.getDefaults().surface_bright,
        R.drawable.blue_round_solid_drawable
    )

    participantCountText.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

}

fun NotificationCardBinding.applyTheme() {

    card.setBackgroundAndColor(

        HMSPrebuiltTheme.getColours()?.surfaceDim,
        HMSPrebuiltTheme.getDefaults().surface_dim,
        R.drawable.blue_round_solid_drawable
    )

    ribbon.setBackgroundColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            HMSPrebuiltTheme.getDefaults().error_default
        )
    )


    crossIcon.drawable.setTint(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    actionButton.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDefault,
        HMSPrebuiltTheme.getDefaults().secondary_default,
        R.drawable.blue_round_solid_drawable
    )

    actionButton.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    heading.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )


}

internal fun ParticipantHeaderItemBinding.applyTheme() {
    with(heading){
        setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                HMSPrebuiltTheme.getDefaults().onsurface_med_emp
            )
        )


    }



}
// ParticipantItem binding
internal fun ListItemPeerListBinding.applyTheme() {
    badNetworkIndicator.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDim,
        HMSPrebuiltTheme.getDefaults().secondary_dim,
        R.drawable.badge_circle_20
    )
    muteUnmuteIcon.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDim,
        HMSPrebuiltTheme.getDefaults().secondary_dim,
        R.drawable.badge_circle_20
    )
    handraise.setBackgroundAndColor(
        HMSPrebuiltTheme.getColours()?.secondaryDim,
        HMSPrebuiltTheme.getDefaults().secondary_dim,
        R.drawable.badge_circle_20
    )
    name.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        )
    )

    peerSettings.setColorFilter(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
        HMSPrebuiltTheme.getDefaults().onsurface_med_emp)
    )
}

internal fun LayoutChatParticipantCombinedBinding.applyTheme() {
    closeCombinedTabButton.setBackgroundDrawable(ResourcesCompat.getDrawable(
        this.root.resources,
        R.drawable.ic_close_16, null
    )?.apply {
        setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                HMSPrebuiltTheme.getDefaults().onsurface_med_emp
            )
        )
    }
    )
    backingLinearLayout.background = ResourcesCompat.getDrawable(this.root.resources,R.drawable.gray_shape_round_dialog, null)!!
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceDim,
                HMSPrebuiltTheme.getDefaults().surface_dim)
            colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
        }

    tabLayout.tabTextColors = ColorStateList(
        arrayOf(
            intArrayOf( android.R.attr.state_selected),
            intArrayOf( -android.R.attr.state_selected)
        ), intArrayOf(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp),
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                HMSPrebuiltTheme.getDefaults().onsurface_low_emp)
        )
    )
    tabLayout.background = getShape()
        //ResourcesCompat.getDrawable(this.root.resources,R.drawable.tab_layout_bg, null)!!
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceDefault,
                HMSPrebuiltTheme.getDefaults().surface_default)
            colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
        }

    val tabGroup = (tabLayout.getChildAt(0) as ViewGroup)
    val chatTab = tabGroup.getChildAt(0)
    val participantTab = tabGroup.getChildAt(1)

    chatTab.background = getTabStateList()
    participantTab.background = getTabStateList()

}

fun getShape(): ShapeDrawable {
    val eightDp = 8.dp().toFloat()
    val lines = floatArrayOf(eightDp,eightDp,eightDp,eightDp,eightDp,eightDp,eightDp,eightDp,eightDp)
    return ShapeDrawable(
        RoundRectShape(
            lines, null,
            null
        )
    )
}
fun LayoutChatParticipantCombinedBinding.getTabStateList(): StateListDrawable {

    val unselectedDrawable = getShape()
        //ResourcesCompat.getDrawable(this.root.resources,R.drawable.k, null)!!
        .apply {
            setTint(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.surfaceDefault,
                    HMSPrebuiltTheme.getDefaults().surface_default)
            )
        }
    val d2= getShape()//ResourcesCompat.getDrawable(this.root.resources,R.drawable.k, null)!!
    .apply {
        setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceBright,
                HMSPrebuiltTheme.getDefaults().surface_bright)
        )
    }
    val selectedInner = InsetDrawable(d2,8)
    val selectedDrawable = LayerDrawable(listOf(unselectedDrawable, selectedInner).toTypedArray())

    val stateList = StateListDrawable()
    stateList.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
    stateList.addState(intArrayOf(-android.R.attr.state_selected), unselectedDrawable)

    return stateList
}

private fun getChatBackgroundDrawable(): ShapeDrawable {
    return getShape()//ResourcesCompat.getDrawable(this.root.resources,R.drawable.send_message_background, null)!!
        .apply {
            val color = getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.surfaceDefault,
                HMSPrebuiltTheme.getDefaults().surface_default)
            colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC)
        }
}

internal fun LayoutChatParticipantCombinedTabChatBinding.applyTheme() {
    // Emptyview

    emptyTitle.setTextColor(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp))

    emptyDescription.setTextColor(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
        HMSPrebuiltTheme.getDefaults().onsurface_med_emp))
    // Chat
    chatView.background = getChatBackgroundDrawable()
    editTextMessage.background = getChatBackgroundDrawable()
    editTextMessage.setTextColor(
        getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp)
    )

    editTextMessage.setHintTextColor(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceLow,
        HMSPrebuiltTheme.getDefaults().onsurface_low_emp))
}

internal fun ListItemChatBinding.applyTheme() {

    name.setTextColor(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
        HMSPrebuiltTheme.getDefaults().onsurface_med_emp))
    message.setTextColor(getColorOrDefault(
        HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
        HMSPrebuiltTheme.getDefaults().onsurface_high_emp))
}

internal fun HlsFragmentLayoutBinding.applyTheme() {
    chatView.background = getChatBackgroundDrawable()
    editTextMessage.background = getChatBackgroundDrawable()
}

internal fun FragmentParticipantsBinding.applyTheme() {
    with(containerSearch) {
        // text color
        // hint color
        // background color
        hintTextColor = ColorStateList.valueOf(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                HMSPrebuiltTheme.getDefaults().onsurface_low_emp
            )
        )
        startIconDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_search_24, null)
            ?.apply { setTint(getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceLow,
                HMSPrebuiltTheme.getDefaults().onsurface_low_emp
            )) }

    }
    with(textInputSearch) {

        setTextColor(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.surfaceBright,
            HMSPrebuiltTheme.getDefaults().surface_bright
        ))
        background = getChatBackgroundDrawable()
    }
}
