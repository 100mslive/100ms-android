package live.hms.roomkit.ui.meeting

import android.graphics.Bitmap
import live.hms.video.plugin.video.virtualbackground.HmsVirtualBackgroundInterface
import live.hms.video.plugin.video.virtualbackground.VideoFrameInfoListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.virtualbackground.HMSVirtualBackground

class HmsVirtualBackgroundInjector(private val hmsSdk : HMSSDK) {
    val vbPlugin : HmsVirtualBackgroundInterface
    init {
        val canLoadClass = try {
            val qualifiedName = HMSVirtualBackground::class.qualifiedName
            Class.forName(qualifiedName)
            true
        } catch (ex : ClassNotFoundException) {
            false
        } catch (ex : NoClassDefFoundError) {
            false
        }

        vbPlugin = if(canLoadClass) HMSVirtualBackground(hmsSdk) else FakeVirtualBackground()

    }
}