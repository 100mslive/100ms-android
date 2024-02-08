package live.hms.roomkit.ui.filters

sealed class VideoFilter {
    object Brightness : VideoFilter()
    object Sharpness : VideoFilter()
    object Saturation : VideoFilter()
    object Redness : VideoFilter()
    object Hue : VideoFilter()
    object Contrast : VideoFilter()
    object Exposure : VideoFilter()
}
