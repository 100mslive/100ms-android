package live.hms.roomkit.ui.meeting

import android.graphics.Bitmap
import android.util.Log
import live.hms.video.plugin.video.HMSVideoPluginType
import live.hms.video.plugin.video.virtualbackground.HmsVirtualBackgroundInterface
import live.hms.video.plugin.video.virtualbackground.VideoFrameInfoListener
import live.hms.video.sdk.HMSPluginResultListener
import org.webrtc.VideoFrame

class FakeVirtualBackground : HmsVirtualBackgroundInterface {
    private fun loggingIgnore(method : String) {
        Log.d("HMSVirtualBackground", "Ignoring method")
    }
    override fun disableEffects() {
        loggingIgnore("disableEffects")
    }

    override fun enableBackground(bitmap: Bitmap) {
        loggingIgnore("enableBackground")
    }

    override fun enableBlur(blurPercentage: Int) {
        loggingIgnore("")
    }

    override fun getCurrentBlurPercentage(): Int {
        loggingIgnore("")
        return 0
    }

    override fun getName(): String =
        "@100mslive/placeholder-hms-virtual-background"

    override fun getPluginType(): HMSVideoPluginType = HMSVideoPluginType.PLACEHOLDER

    override suspend fun init() {
        loggingIgnore("init")
    }

    override fun isSupported(): Boolean = false

    override fun processVideoFrame(
        input: VideoFrame,
        outputListener: HMSPluginResultListener?,
        skipProcessing: Boolean?
    ) {
        loggingIgnore("processVideoFrame")
    }

    override fun setVideoFrameInfoListener(listener: VideoFrameInfoListener) {
        loggingIgnore("setVideoFrameInfoListener")
    }

    override fun stop() {
        loggingIgnore("stop")
    }
}