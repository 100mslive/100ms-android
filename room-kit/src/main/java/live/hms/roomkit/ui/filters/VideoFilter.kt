package live.hms.roomkit.ui.filters

sealed class VideoFilter {
    object Brightness : VideoFilter()
    object Blur : VideoFilter()
    object Quality : VideoFilter()
    object Sharpness : VideoFilter()
    object Saturation : VideoFilter()
    object Redness : VideoFilter()
    object Smoothness : VideoFilter()
    object Hue : VideoFilter()
    object Contrast : VideoFilter()
    object Exposure : VideoFilter()
}
