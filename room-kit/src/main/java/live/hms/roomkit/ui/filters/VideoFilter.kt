package live.hms.roomkit.ui.filters

sealed class VideoFilter {
    object Brightness :  VideoFilter()
    object Sharpness :  VideoFilter()
    object Saturation :  VideoFilter()
}
