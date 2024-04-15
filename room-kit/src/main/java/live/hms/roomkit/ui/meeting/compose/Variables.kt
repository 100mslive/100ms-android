package live.hms.roomkit.ui.meeting.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.getColorOrDefault

class Variables {
    companion object {
        val Spacing2 = 16.dp
        val Spacing1 = 8.dp
        val Spacing0 = 4.dp
        val TwelveDp = 12.dp
        val PrimaryDefault : Color = Color(getColorOrDefault(
            HMSPrebuiltTheme.getColours()?.primaryDefault,
            HMSPrebuiltTheme.getDefaults().primary_default))

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
    }
}
