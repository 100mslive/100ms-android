package live.hms.roomkit.ui.meeting.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault

class Variables {
    companion object {
        val Spacing3: Dp = 24.dp
        val Spacing4: Dp = 32.dp
        val Spacing2 = 16.dp
        val Spacing1 = 8.dp
        val Spacing0 = 4.dp
        val TwelveDp = 12.dp
        val PrimaryDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default))

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
        val BorderBright = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.borderBright,
            HMSPrebuiltTheme.getDefaults().border_bright
        ))

        val BackgroundDim = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.backgroundDim,
            HMSPrebuiltTheme.getDefaults().background_dim
        )).copy(alpha = 0.64f) // backrground dim is always 64% transparent.

        val SurfaceDim: Color = Color(0xFF11131A)
    }
}
