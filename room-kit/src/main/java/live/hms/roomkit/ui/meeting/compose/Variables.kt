package live.hms.roomkit.ui.meeting.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault

class Variables {
    companion object {
        val Spacing1 = 4.dp
        val Spacing2 = 8.dp
        val Spacing3 = 12.dp
        val Spacing4 = 16.dp
        val Spacing5 = 20.dp
        val Spacing6 = 24.dp
        val Spacing7 = 28.dp
        val Spacing8 = 32.dp
        val Spacing9 = 36.dp
        val Spacing10 = 40.dp
        val PrimaryDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default))

        val PrimaryDisabled : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDisabled,
            HMSPrebuiltTheme.getDefaults().primary_disabled,
        ))

        val SurfaceDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.surfaceDefault,
            HMSPrebuiltTheme.getDefaults().surface_default
        ))
        val SecondaryDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.secondaryDefault,
            HMSPrebuiltTheme.getDefaults().secondary_default))

        val AlertErrorDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.alertErrorDefault,
            "0xFFC74E5B"))

        val OnPrimaryHigh : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onPrimaryHigh,
            HMSPrebuiltTheme.getDefaults().onprimary_high_emp
        ))

        val OnSurfaceHigh: Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
            HMSPrebuiltTheme.getDefaults().onsurface_high_emp
        ))
        val OnSecondaryHigh = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSecondaryHigh,
            HMSPrebuiltTheme.getDefaults().onsecondary_high_emp
        ))
        val OnSurfaceMedium = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
            HMSPrebuiltTheme.getDefaults().onsurface_med_emp
        ))
        val OnSurfaceLow = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.onSurfaceLow,
            HMSPrebuiltTheme.getDefaults().onsurface_low_emp
        ))
        val BorderBright = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderBright,
            HMSPrebuiltTheme.getDefaults().border_bright
        ))
        val BorderDefault = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderDefault,
            HMSPrebuiltTheme.getDefaults().border_default
        ))

        val BackgroundDim = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_dim
        )).copy(alpha = 0.64f) // backrground dim is always 64% transparent.

        val SurfaceDim: Color = Color(0xFF11131A)
    }
}
