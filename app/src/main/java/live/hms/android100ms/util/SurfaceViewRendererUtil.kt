package live.hms.android100ms.util

import com.brytecam.lib.webrtc.HMSWebRTCEglUtils
import live.hms.android100ms.BuildConfig
import live.hms.android100ms.ui.meeting.MeetingTrack
import org.webrtc.SurfaceViewRenderer

object SurfaceViewRendererUtil {

  fun bind(view: SurfaceViewRenderer, item: MeetingTrack): Boolean {
    if (item.videoTrack == null) return false

    view.apply {
      val context = HMSWebRTCEglUtils.getRootEglBaseContext()

      if (BuildConfig.DEBUG && context == null) {
        error("Received HMSWebRTCEglUtils=NULL")
      }

      init(context, null)
      item.videoTrack.addSink(this)
    }

    return true
  }

  fun unbind(view: SurfaceViewRenderer, item: MeetingTrack): Boolean {
    if (item.videoTrack == null) return false

    view.apply {
      // NOTE: We don't dispose off the MediaStreamTrack here as it can
      // be re-used by the any other view

      item.videoTrack.removeSink(this)
      release()
    }

    return true
  }

}